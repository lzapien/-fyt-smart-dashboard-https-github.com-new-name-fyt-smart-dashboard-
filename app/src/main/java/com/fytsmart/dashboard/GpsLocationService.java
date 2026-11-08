package com.fytsmart.dashboard;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import java.util.LinkedHashSet;
import java.util.Set;

/** Sole owner of the app's GPS request. Bound UI clients never register a LocationManager listener. */
public final class GpsLocationService extends Service implements LocationListener {
    static final String TAG = "FYT_GPS";
    private static final String CHANNEL = "fyt_continuous_gps";
    private static final String START = "com.fytsmart.dashboard.GPS_START", STOP = "com.fytsmart.dashboard.GPS_STOP";
    private static final int NOTIFICATION = 1001;
    private static volatile boolean foregroundRunning;
    enum Status {
        WAITING("Esperando señal GPS"), ACTIVE("GPS activo"), STALE("Sin señal GPS reciente"),
        DISABLED("GPS apagado"), NO_PERMISSION("Permiso GPS pendiente"), UNAVAILABLE("GPS no disponible");
        final String description;
        Status(String description) { this.description = description; }
    }
    interface Listener {
        void onGpsLocation(Location location);
        void onGpsStatus(Status status);
    }
    final class LocalBinder extends Binder { GpsLocationService service() { return GpsLocationService.this; } }
    private final LocalBinder binder = new LocalBinder();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Listener> listeners = new LinkedHashSet<>();
    private LocationManager manager;
    private DashboardPreferences settings;
    private GpsUpdatePolicy policy;
    private Location lastFix;
    private Status status = Status.WAITING;
    private boolean registered, bound, providersRegistered;
    private long requestedInterval = -1, receivedCount;
    private final BroadcastReceiver providers = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { ensureUpdates(); }
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener preferencesChanged = (prefs, key) -> {
        if (DashboardPreferences.GPS_MOVING.equals(key) || DashboardPreferences.GPS_IDLE.equals(key)) {
            policy = new GpsUpdatePolicy(settings.movingMs(), settings.idleMs());
            ensureUpdates();
        } else if (DashboardPreferences.GPS_CONTINUOUS.equals(key) && !settings.continuousGps()) stopContinuous();
    };
    private final Runnable validity = new Runnable() {
        @Override public void run() {
            if (!registered) return;
            if (!hasPermission()) { permissionLost(); return; }
            if (lastFix != null && !fresh(lastFix)) {
                policy.signalLost(); setStatus(Status.STALE); ensureUpdates();
            }
            main.removeCallbacks(this);
            if (registered) main.postDelayed(this, 5_000);
        }
    };

    static boolean hasPermission(Context context) {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    /** Only called by a resumed, user-visible activity. Never from a boot/screen receiver. */
    static void startContinuousFromVisible(Context context) {
        if (!hasPermission(context) || !new DashboardPreferences(context).continuousGps() || foregroundRunning) return;
        try { context.startForegroundService(new Intent(context, GpsLocationService.class).setAction(START)); }
        catch (RuntimeException denied) { Log.w(TAG, "Foreground GPS start denied; visible-only binding remains available", denied); }
    }
    static void stopContinuousFromVisible(Context context) {
        if (!foregroundRunning) return;
        try { context.startService(new Intent(context, GpsLocationService.class).setAction(STOP)); }
        catch (RuntimeException denied) { Log.w(TAG, "GPS stop request failed", denied); }
    }
    @Override public void onCreate() {
        super.onCreate(); settings = new DashboardPreferences(this);
        manager = (LocationManager)getSystemService(LOCATION_SERVICE);
        policy = new GpsUpdatePolicy(settings.movingMs(), settings.idleMs());
        settings.values.registerOnSharedPreferenceChangeListener(preferencesChanged);
        NotificationManager notifications = getSystemService(NotificationManager.class);
        if (notifications != null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Ubicación continua del dashboard", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible mientras el dashboard solicita lecturas GPS reales");
            notifications.createNotificationChannel(channel);
        }
        try {
            IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(providers, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(providers, filter);
            providersRegistered = true;
        } catch (RuntimeException unavailable) { Log.w(TAG, "Provider-change receiver unavailable", unavailable); }
        DashboardRecoveryController.get(this);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && STOP.equals(intent.getAction())) {
            settings.values.edit().putBoolean(DashboardPreferences.GPS_CONTINUOUS, false).apply();
            stopContinuous(); return START_NOT_STICKY;
        }
        if (!settings.continuousGps() || !hasPermission()) {
            if (!hasPermission()) setStatus(Status.NO_PERMISSION);
            stopContinuous(); return START_NOT_STICKY;
        }
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(NOTIFICATION, notification());
            foregroundRunning = true;
            ensureUpdates();
            Log.i(TAG, "Continuous GPS active; one listener; requested movingMs=" + settings.movingMs()
                    + " idleMs=" + settings.idleMs() + "; actual delivery depends on GNSS/Android");
            return START_STICKY;
        } catch (RuntimeException denied) {
            Log.w(TAG, "Cannot enter location foreground service", denied);
            setStatus(Status.UNAVAILABLE); stopContinuous(); return START_NOT_STICKY;
        }
    }
    @Override public IBinder onBind(Intent intent) { bound = true; ensureUpdates(); return binder; }
    @Override public boolean onUnbind(Intent intent) {
        bound = false; listeners.clear();
        if (!foregroundRunning) stopUpdates();
        return true;
    }
    @Override public void onRebind(Intent intent) { super.onRebind(intent); bound = true; ensureUpdates(); }
    void addListener(Listener listener) {
        if (!listeners.add(listener)) return;
        listener.onGpsStatus(status);
        if (lastFix != null && fresh(lastFix)) listener.onGpsLocation(new Location(lastFix));
    }
    void removeListener(Listener listener) { listeners.remove(listener); }
    private boolean hasPermission() { return hasPermission(this); }
    private boolean fresh(Location location) {
        long now = SystemClock.elapsedRealtimeNanos(), fix = location.getElapsedRealtimeNanos();
        return fix > 0 && now >= fix && now - fix <= GpsSpeedState.MAX_AGE_NANOS;
    }
    private void ensureUpdates() {
        if (!bound && !foregroundRunning) return;
        if (!hasPermission()) { permissionLost(); return; }
        if (manager == null) { setStatus(Status.UNAVAILABLE); return; }
        try {
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                stopUpdates(); lastFix = null; policy.signalLost(); setStatus(Status.DISABLED); return;
            }
            long interval = policy.intervalMs();
            if (registered && requestedInterval == interval) return;
            // Remove the previous request first, even when only its rate changes.
            if (!stopUpdates()) return;
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, 0, this, Looper.getMainLooper());
            registered = true; requestedInterval = interval;
            Log.i(TAG, "GPS listener registered requestedIntervalMs=" + interval + " clients=" + listeners.size());
            if (lastFix == null) {
                setStatus(Status.WAITING);
                Location cached = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (cached != null && fresh(cached)) onLocationChanged(cached);
            }
            main.removeCallbacks(validity); main.postDelayed(validity, 5_000);
        } catch (SecurityException denied) { permissionLost(); }
        catch (RuntimeException unavailable) { stopUpdates(); setStatus(Status.UNAVAILABLE); Log.w(TAG, "GPS request failed", unavailable); }
    }
    private boolean stopUpdates() {
        main.removeCallbacks(validity);
        if (registered && manager != null) {
            try { manager.removeUpdates(this); }
            catch (RuntimeException unavailable) { Log.w(TAG, "GPS listener removal failed; refusing a duplicate request", unavailable); return false; }
        }
        registered = false; requestedInterval = -1; return true;
    }
    private void stopContinuous() {
        if (foregroundRunning) { foregroundRunning = false; stopForeground(true); }
        stopSelf();
        if (!bound) stopUpdates();
        Log.i(TAG, "Continuous GPS stopped; visible-only updates=" + bound);
    }
    private void permissionLost() {
        stopUpdates(); lastFix = null; policy.signalLost(); setStatus(Status.NO_PERMISSION);
        stopContinuous();
    }
    @Override public void onLocationChanged(Location location) {
        if (!hasPermission()) { permissionLost(); return; }
        if (location == null || !LocationManager.GPS_PROVIDER.equals(location.getProvider()) || !fresh(location)
                || !Double.isFinite(location.getLatitude()) || !Double.isFinite(location.getLongitude())
                || Math.abs(location.getLatitude()) > 90 || Math.abs(location.getLongitude()) > 180) return;
        if (lastFix != null && location.getElapsedRealtimeNanos() <= lastFix.getElapsedRealtimeNanos()) return;
        lastFix = new Location(location); receivedCount++;
        policy.onFix(true, location.hasSpeed(), location.getSpeed(), location.hasAccuracy(), location.getAccuracy());
        setStatus(Status.ACTIVE);
        for (Listener listener : new LinkedHashSet<>(listeners)) listener.onGpsLocation(new Location(lastFix));
        Log.d(TAG, "GPS fix received count=" + receivedCount + " requestedIntervalMs=" + policy.intervalMs()
                + " ageMs=" + (SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos()) / 1_000_000);
        ensureUpdates();
    }
    @Override public void onProviderDisabled(String provider) { if (LocationManager.GPS_PROVIDER.equals(provider)) ensureUpdates(); }
    @Override public void onProviderEnabled(String provider) { if (LocationManager.GPS_PROVIDER.equals(provider)) ensureUpdates(); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { /* Fix freshness is authoritative. */ }
    private void setStatus(Status value) {
        boolean changed = status != value; status = value;
        if (changed) {
            for (Listener listener : new LinkedHashSet<>(listeners)) listener.onGpsStatus(value);
            Log.i(TAG, "GPS status=" + value);
        }
        if (foregroundRunning) {
            NotificationManager notifications = getSystemService(NotificationManager.class);
            if (notifications != null) notifications.notify(NOTIFICATION, notification());
        }
    }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, GpsLocationService.class).setAction(STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = status.description;
        if (status == Status.ACTIVE && lastFix != null && lastFix.hasAccuracy() && Float.isFinite(lastFix.getAccuracy()))
            text += " · precisión " + Math.round(lastFix.getAccuracy()) + " m";
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_gps_notification)
                .setContentTitle("GPS de FYT Smart Dashboard").setContentText(text).setContentIntent(open)
                .setCategory(Notification.CATEGORY_SERVICE).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "Detener GPS continuo", stop).build()).build();
    }
    @Override public void onDestroy() {
        stopUpdates(); main.removeCallbacksAndMessages(null); listeners.clear();
        settings.values.unregisterOnSharedPreferenceChangeListener(preferencesChanged);
        if (providersRegistered) unregisterReceiver(providers);
        foregroundRunning = false; stopForeground(true);
        Log.i(TAG, "GPS service destroyed; listeners released"); super.onDestroy();
    }
}
