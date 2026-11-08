package com.fytsmart.dashboard;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AtomicFile;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** App launch requests only. No FYT audio switching, firmware IPC or foreground observer. */
public final class SourceManager implements AutoCloseable {
    private static final String TAG = "FYT_SourceManager";
    private static final String DEFAULT_JSON = """
            {"version":1,"debounce_ms":500,"wrap_around":true,"apps":[
              {"name":"Radio Navimods","package":"com.navimods.radio","enabled":true},
              {"name":"Amazon Music","package":"com.amazon.mp3","enabled":true},
              {"name":"Waze","package":"com.waze","enabled":true},
              {"name":"YouTube Music","package":"com.google.android.apps.youtube.music","enabled":true},
              {"name":"Google Maps","package":"com.google.android.apps.maps","enabled":true}]}
            """;

    public interface Listener {
        /** Delivered on the main thread; describes requests, never confirmed audio changes. */
        void onMessage(String message);
    }

    private static final class Config {
        final List<SourceCycle.App> apps;
        final long debounceMs;
        final boolean wrap;
        Config(List<SourceCycle.App> apps, long debounceMs, boolean wrap) {
            this.apps = apps;
            this.debounceMs = debounceMs;
            this.wrap = wrap;
        }
    }

    private final Context context;
    private final AtomicFile configFile;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile Listener listener;
    private volatile boolean closed;
    private volatile long debounceMs = 500;
    private volatile String configurationJson = DEFAULT_JSON;
    private volatile String lastRequestedPackage;
    private volatile String lastAcceptedPackage;
    private volatile String lastSelectedPackage;
    private boolean busy;
    private long lastPress = -1;

    public SourceManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        configFile = new AtomicFile(new File(this.context.getFilesDir(), "source_apps.json"));
        worker.execute(() -> {
            try {
                SharedPreferences prefs = preferences();
                lastRequestedPackage = prefs.getString("last_requested_package", null);
                lastAcceptedPackage = prefs.getString("last_accepted_package", null);
                lastSelectedPackage = prefs.getString("last_selected_package", null);
                loadConfiguration();
            } catch (RuntimeException e) {
                Log.e(TAG, "Initialization failed", e);
                message("SOURCE: error al inicializar; revisa Logcat");
            }
        });
    }

    public SourceManager(Context context) { this(context, null); }

    /** Thread-safe asynchronous entry point for the virtual button or a future event adapter. */
    public synchronized void nextSource() {
        if (closed) return;
        long now = SystemClock.elapsedRealtime();
        if (busy || (lastPress >= 0 && now - lastPress < debounceMs)) {
            Log.i(TAG, "Request ignored: debounce or request in progress");
            return;
        }
        lastPress = now;
        busy = true;
        worker.execute(() -> {
            try {
                launchNext(loadConfiguration());
            } catch (RuntimeException e) {
                Log.e(TAG, "SOURCE request failed", e);
                message("SOURCE: no se pudo procesar la solicitud");
            } finally {
                synchronized (SourceManager.this) { busy = false; }
            }
        });
    }

    /** Shared launch path for existing shortcuts; source order is unaffected. */
    public synchronized void launchApplication(String name, String packageName) {
        if (closed) return;
        worker.execute(() -> {
            if (!tryLaunch(context.getPackageManager(), new SourceCycle.App(name, packageName, true), false)) {
                message("No se pudo abrir " + name);
            }
        });
    }

    /** Snapshot only: no file I/O on caller thread. Future editors can use this plus saveConfiguration. */
    public String getConfigurationJson() { return configurationJson; }

    /** Asynchronous validation and atomic save; malformed input never replaces the existing file. */
    public synchronized void saveConfiguration(String json) {
        if (closed) return;
        worker.execute(() -> {
            try {
                Config config = parse(json);
                write(json);
                configurationJson = json;
                debounceMs = config.debounceMs;
                message("SOURCE: configuración guardada");
            } catch (IOException | JSONException | RuntimeException e) {
                Log.e(TAG, "Configuration rejected; file preserved", e);
                message("SOURCE: configuración inválida o no guardada");
            }
        });
    }

    public String getLastRequestedPackage() { return lastRequestedPackage; }
    /** startActivity returned without exception; this is NOT foreground or audio confirmation. */
    public String getLastAcceptedPackage() { return lastAcceptedPackage; }
    /** Unknown until a future, independently verified foreground observer is implemented. */
    public String getLastConfirmedPackage() { return null; }

    private SharedPreferences preferences() {
        return context.getSharedPreferences("source_manager", Context.MODE_PRIVATE);
    }

    private Config loadConfiguration() {
        try {
            String json;
            if (!exists(configFile)) {
                json = migrateLegacyOrDefault();
                parse(json);
                write(json);
            } else {
                json = new String(configFile.readFully(), StandardCharsets.UTF_8);
            }
            Config config = parse(json);
            configurationJson = json;
            debounceMs = config.debounceMs;
            return config;
        } catch (IOException | JSONException | RuntimeException e) {
            // Do not overwrite damaged files. Use defaults in memory so the dashboard remains usable.
            Log.e(TAG, "Configuration unreadable; preserved on disk, using defaults in memory", e);
            message("SOURCE: configuración dañada; usando valores predeterminados");
            try {
                Config config = parse(DEFAULT_JSON);
                configurationJson = DEFAULT_JSON;
                debounceMs = config.debounceMs;
                return config;
            } catch (JSONException impossible) {
                throw new IllegalStateException("Invalid built-in configuration", impossible);
            }
        }
    }

    private String migrateLegacyOrDefault() throws IOException, JSONException {
        AtomicFile legacy = new AtomicFile(new File(context.getFilesDir(), "sources.json"));
        if (!exists(legacy)) return DEFAULT_JSON;
        JSONObject old = new JSONObject(new String(legacy.readFully(), StandardCharsets.UTF_8));
        int version = old.getInt("version");
        if (version != 1 && version != 2) throw new JSONException("Unknown legacy version");
        JSONArray sources = old.getJSONArray("sources");
        JSONArray apps = new JSONArray();
        String oldSelectedId = preferences().getString("last_requested_id", "");
        Set<String> ids = new HashSet<>();
        String selected = null;
        for (int i = 0; i < sources.length(); i++) {
            JSONObject entry = sources.getJSONObject(i);
            String id = entry.getString("id");
            if (id.trim().isEmpty() || !ids.add(id)) throw new JSONException("Invalid legacy ID");
            String pkg = entry.getString("package");
            String name = entry.getString("name");
            boolean enabled = strictBoolean(entry, "enabled");
            if ("radio".equals(id) && "Radio FM".equals(name) && pkg.isEmpty() && !enabled) {
                pkg = "com.navimods.radio";
                name = "Radio Navimods";
                enabled = true;
            }
            apps.put(new JSONObject().put("name", name).put("package", pkg).put("enabled", enabled));
            if (id.equals(oldSelectedId)) selected = pkg;
        }
        String json = new JSONObject().put("version", 1).put("debounce_ms", 500)
                .put("wrap_around", true).put("apps", apps).toString(2);
        parse(json);
        if (lastSelectedPackage == null && selected != null) {
            lastSelectedPackage = selected;
            preferences().edit().putString("last_selected_package", selected).apply();
        }
        Log.i(TAG, "Migrated legacy sources.json; original retained");
        return json;
    }

    private static boolean exists(AtomicFile file) {
        return file.getBaseFile().exists() || new File(file.getBaseFile().getPath() + ".bak").exists();
    }

    private void write(String json) throws IOException {
        FileOutputStream stream = null;
        try {
            stream = configFile.startWrite();
            stream.write(json.getBytes(StandardCharsets.UTF_8));
            configFile.finishWrite(stream);
        } catch (IOException | RuntimeException e) {
            if (stream != null) configFile.failWrite(stream);
            throw e;
        }
    }

    private static Config parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!(root.get("version") instanceof Integer) || root.getInt("version") != 1)
            throw new JSONException("Unsupported configuration version");
        Object debounce = root.get("debounce_ms");
        if (!(debounce instanceof Integer || debounce instanceof Long)
                || ((Number) debounce).longValue() < 0 || ((Number) debounce).longValue() > 60000)
            throw new JSONException("debounce_ms must be an integer between 0 and 60000");
        boolean wrap = strictBoolean(root, "wrap_around");
        JSONArray array = root.getJSONArray("apps");
        List<SourceCycle.App> apps = new ArrayList<>();
        Set<String> packages = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            if (!(entry.get("name") instanceof String) || !(entry.get("package") instanceof String))
                throw new JSONException("name and package must be strings");
            String name = entry.getString("name");
            String pkg = entry.getString("package");
            if (name.trim().isEmpty() || !pkg.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")
                    || !packages.add(pkg)) throw new JSONException("Invalid or duplicate app");
            apps.add(new SourceCycle.App(name, pkg, strictBoolean(entry, "enabled")));
        }
        return new Config(apps, ((Number) debounce).longValue(), wrap);
    }

    private static boolean strictBoolean(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof Boolean)) throw new JSONException(key + " must be boolean");
        return (Boolean) value;
    }

    private void launchNext(Config config) {
        PackageManager pm = context.getPackageManager();
        for (SourceCycle.App app : SourceCycle.candidates(config.apps, lastSelectedPackage, config.wrap)) {
            if (closed) return;
            if (tryLaunch(pm, app, true)) return;
        }
        Log.i(TAG, "No next enabled, accessible application (wrap=" + config.wrap + ")");
        message("SOURCE: no hay siguiente aplicación disponible");
    }

    private boolean tryLaunch(PackageManager pm, SourceCycle.App app, boolean select) {
        if (closed) return false;
        try {
            if (!pm.getApplicationInfo(app.packageName, 0).enabled) {
                Log.i(TAG, "Skipped disabled package: " + app.packageName);
                return false;
            }
            Intent launch = launchIntent(pm, app.packageName);
            ActivityInfo activity = launch == null ? null : launch.resolveActivityInfo(pm, 0);
            if (!accessible(activity)) {
                Log.i(TAG, "Skipped: no accessible launcher for " + app.packageName);
                return false;
            }
            if (closed) return false;
            lastRequestedPackage = app.packageName;
            preferences().edit().putString("last_requested_package", lastRequestedPackage).apply();
            Log.i(TAG, "Launch requested: " + app.packageName + " component=" + launch.getComponent());
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
            lastAcceptedPackage = app.packageName;
            SharedPreferences.Editor editor = preferences().edit()
                    .putString("last_accepted_package", lastAcceptedPackage);
            if (select) {
                lastSelectedPackage = app.packageName;
                editor.putString("last_selected_package", lastSelectedPackage);
            }
            editor.apply();
            Log.i(TAG, "Launch request accepted: " + app.packageName
                    + "; foreground unconfirmed; FYT audio source unconfirmed");
            if (select) message("Apertura solicitada: " + app.name);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "Skipped not installed or not visible: " + app.packageName);
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Launch denied/unavailable: " + app.packageName, e);
        } catch (RuntimeException e) {
            Log.e(TAG, "Launch failed: " + app.packageName, e);
        }
        return false;
    }

    private boolean accessible(ActivityInfo activity) {
        return activity != null && activity.exported && activity.enabled && activity.applicationInfo.enabled
                && (activity.permission == null || context.checkSelfPermission(activity.permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    private Intent launchIntent(PackageManager pm, String pkg) {
        Intent standard = pm.getLaunchIntentForPackage(pkg);
        if (!"com.navimods.radio".equals(pkg)) return standard;
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(pkg);
        List<ResolveInfo> candidates = pm.queryIntentActivities(query, 0);
        candidates.sort((a, b) -> a.activityInfo.name.compareTo(b.activityInfo.name));
        Intent fallback = null;
        for (ResolveInfo candidate : candidates) {
            ActivityInfo activity = candidate.activityInfo;
            if (!pkg.equals(activity.packageName) || !accessible(activity)
                    || "com.navimods.radio.ServiceActivity".equals(activity.name)
                    || "com.navimods.radio.ServiceActivity".equals(activity.targetActivity)) continue;
            Intent launch = new Intent(query).setComponent(new ComponentName(pkg, activity.name));
            if ("com.navimods.radio.RadioActivity".equals(activity.name)) return launch;
            if (standard != null && launch.getComponent().equals(standard.getComponent())) fallback = launch;
            else if (fallback == null) fallback = launch;
        }
        return fallback;
    }

    private void message(String message) {
        if (closed) return;
        main.post(() -> {
            Listener target = listener;
            if (!closed && target != null) target.onMessage(message);
        });
    }

    @Override public synchronized void close() {
        closed = true;
        listener = null;
        main.removeCallbacksAndMessages(null);
        worker.shutdown();
    }
}
