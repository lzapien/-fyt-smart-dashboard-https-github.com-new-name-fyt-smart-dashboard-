package com.fytsmart.dashboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Internal, offline app drawer. MAIN/HOME always belongs to MainActivity. */
public final class AppDrawerActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), task -> new Thread(task, "FYT-AppDrawer"));
    private final LruCache<String, Bitmap> icons = new LruCache<String, Bitmap>(4 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getAllocationByteCount(); }
    };
    private final List<LauncherAppCatalog.App> allApps = new ArrayList<>();
    private final List<LauncherAppCatalog.App> visibleApps = new ArrayList<>();
    private final Set<String> iconsPending = new HashSet<>();
    private DashboardPalette displayPalette=DashboardPalette.DARK;
    private DisplayModeController displayController;
    private LinearLayout drawerRoot;
    private Button backButton;
    private void applyDisplayPalette(DashboardPalette palette) {
        displayPalette=palette;drawerRoot.setBackgroundColor(palette.background);
        getWindow().setNavigationBarColor(palette.background);getWindow().setStatusBarColor(palette.background);
        DisplayUi.apply(drawerRoot,palette);backButton.setBackground(card());
        search.setBackground(shape(palette.light?palette.panelTop:0xff131b24,palette.light?palette.border:0xff263a4a));
        search.setBackgroundTintList(null);adapter.notifyDataSetChanged();
    }
    private EditText search;
    private GridView grid;
    private TextView empty, count;
    private AppsAdapter adapter;
    private int generation, savedPosition;
    private boolean active, loading, launchPending;
    private long lastLaunch;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        displayController=new DisplayModeController(this,this::applyDisplayPalette);
        displayPalette=displayController.current();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(Color.BLACK);
        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        LinearLayout root = new LinearLayout(this);drawerRoot=root;
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));
        root.setFocusableInTouchMode(true);
        root.setTag("app_drawer_root");
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this);backButton=back;
        back.setTag("drawer_back");
        back.setText("Volver");
        back.setTextSize(16);
        back.setTextColor(0xfff6f8fc);
        back.setAllCaps(false);
        back.setBackground(card());
        back.setOnClickListener(view -> returnToDashboard());
        header.addView(back, new LinearLayout.LayoutParams(dp(128), dp(52)));
        TextView title = text(24, 0xfff6f8fc);
        title.setText("Aplicaciones");
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(dp(20), 0, 0, 0);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        count = text(14, 0xffa9bbd0);
        header.addView(count, new LinearLayout.LayoutParams(-2, dp(56)));
        root.addView(header);

        search = new EditText(this);
        search.setTag("drawer_search");
        search.setHint("Buscar aplicaciones");
        search.setContentDescription("Buscar por nombre o paquete");
        search.setSingleLine(true);
        search.setTextSize(18);
        search.setTextColor(0xfff6f8fc);
        search.setHintTextColor(0xffa9bbd0);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setBackground(shape(0xff131b24, 0xff263a4a));
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); return true; }
            return false;
        });
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(52));
        searchParams.setMargins(0, dp(12), 0, dp(16));
        root.addView(search, searchParams);

        FrameLayout content = new FrameLayout(this);
        grid = new GridView(this);
        grid.setTag("drawer_grid");
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(dp(136));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(dp(12));
        grid.setVerticalSpacing(dp(12));
        grid.setPadding(0, 0, 0, dp(4));
        grid.setClipToPadding(false);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        adapter = new AppsAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> openApp(visibleApps.get(position)));
        content.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        empty = text(18, 0xffa9bbd0);
        empty.setTag("drawer_empty");
        empty.setGravity(Gravity.CENTER);
        content.addView(empty, new FrameLayout.LayoutParams(-1, -1));
        grid.setEmptyView(empty);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        if (state != null) {
            search.setText(state.getString("query", ""));
            savedPosition = state.getInt("position", 0);
        }
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(); }
            @Override public void afterTextChanged(Editable value) {}
        });
        root.requestFocus();
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(int sp, int color) {
        TextView view = new TextView(this);
        view.setTextSize(sp); view.setTextColor(color);DisplayUi.apply(view,displayPalette); view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }
    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color); result.setCornerRadius(dp(16)); result.setStroke(dp(1), stroke);
        return result;
    }
    private Drawable card() {
        return new RippleDrawable(ColorStateList.valueOf(displayPalette.light?0x33005a91:0x334cb8db),
                shape(displayPalette.light?displayPalette.panelTop:0xff151b22,displayPalette.light?displayPalette.border:0xff2a3540),shape(Color.WHITE,Color.WHITE));
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override protected void onResume() {
        super.onResume();displayController.start(); active = true; launchPending = false; refreshApps();
    }
    @Override protected void onPause() {
        displayController.stop();savedPosition = grid.getFirstVisiblePosition();
        active = false; generation++;
        worker.getQueue().clear(); main.removeCallbacksAndMessages(null); iconsPending.clear();
        super.onPause();
    }
    @Override protected void onDestroy() {
        if(displayController!=null)displayController.close();active = false; generation++; worker.shutdownNow(); main.removeCallbacksAndMessages(null);
        icons.evictAll(); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("query", search.getText().toString());
        state.putInt("position", grid.getFirstVisiblePosition());
        super.onSaveInstanceState(state);
    }
    @Override public void onBackPressed() { returnToDashboard(); }
    private void returnToDashboard() { hideKeyboard(); finish(); }
    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
    }

    private void refreshApps() {
        final int request = ++generation;
        loading = true;
        worker.getQueue().clear(); iconsPending.clear(); icons.evictAll();
        allApps.clear(); filter();
        final Context appContext = getApplicationContext();
        worker.execute(() -> {
            List<LauncherAppCatalog.App> loaded;
            String failure = null;
            try { loaded = LauncherAppCatalog.load(appContext); }
            catch (RuntimeException error) {
                Log.w(LauncherAppCatalog.TAG, "Cannot query launcher apps", error);
                loaded = new ArrayList<>(); failure = "Aplicaciones no disponibles";
            }
            final List<LauncherAppCatalog.App> result = loaded;
            final String message = failure;
            main.post(() -> {
                if (!active || generation != request) return;
                loading = false; allApps.clear(); allApps.addAll(result); filter();
                grid.setSelection(Math.min(savedPosition, Math.max(0, visibleApps.size() - 1)));
                if (message != null) empty.setText(message);
                Log.i(LauncherAppCatalog.TAG, "Loaded " + allApps.size() + " accessible launcher packages");
            });
        });
    }
    private void filter() {
        visibleApps.clear();
        String query = search.getText().toString();
        for (LauncherAppCatalog.App app : allApps) {
            if (AppDrawerSearch.matches(app.label, app.component.getPackageName(), query)) visibleApps.add(app);
        }
        empty.setText(loading ? "Cargando aplicaciones…" : allApps.isEmpty()
                ? "No hay aplicaciones disponibles" : "Sin resultados");
        count.setText(loading ? "" : Integer.toString(visibleApps.size()));
        adapter.notifyDataSetChanged();
    }

    private void openApp(LauncherAppCatalog.App app) {
        long now = SystemClock.elapsedRealtime();
        if (!active || launchPending || (lastLaunch != 0 && now - lastLaunch < 700)) return;
        lastLaunch = now; launchPending = true; hideKeyboard();
        final int request = generation;
        final Context appContext = getApplicationContext();
        Log.i(LauncherAppCatalog.TAG, "Launch requested: " + app.component.flattenToShortString());
        worker.execute(() -> {
            try {
                Intent intent = LauncherAppCatalog.launchIntent(appContext, app);
                main.post(() -> {
                    if (!active || generation != request) return;
                    try {
                        startActivity(intent);
                        Log.i(LauncherAppCatalog.TAG, "Activity launch accepted: " + app.component
                                + "; media-source change not confirmed");
                    } catch (RuntimeException error) { launchFailed(app, error); }
                    finally { launchPending = false; }
                });
            } catch (PackageManager.NameNotFoundException | RuntimeException error) {
                main.post(() -> {
                    if (active && generation == request) launchFailed(app, error);
                });
            }
        });
    }
    private void launchFailed(LauncherAppCatalog.App app, Exception error) {
        launchPending = false;
        Log.w(LauncherAppCatalog.TAG, "Launch failed: " + app.component, error);
        Toast.makeText(this, "No se pudo abrir " + app.label, Toast.LENGTH_SHORT).show();
        refreshApps();
    }

    private final class AppsAdapter extends BaseAdapter {
        @Override public int getCount() { return visibleApps.size(); }
        @Override public LauncherAppCatalog.App getItem(int position) { return visibleApps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View recycled, ViewGroup parent) {
            LinearLayout tile;
            if (recycled instanceof LinearLayout) tile = (LinearLayout)recycled;
            else {
                tile = new LinearLayout(AppDrawerActivity.this);
                tile.setOrientation(LinearLayout.VERTICAL); tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(8), dp(10), dp(8), dp(8));
                tile.setLayoutParams(new GridView.LayoutParams(-1, dp(128)));
                tile.setBackground(card()); tile.setClipToOutline(true);
                ImageView image = new ImageView(AppDrawerActivity.this);
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                tile.addView(image, new LinearLayout.LayoutParams(dp(60), dp(60)));
                TextView name = text(15, 0xfff6f8fc);
                name.setGravity(Gravity.CENTER); name.setMaxLines(2); name.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, dp(44));
                labelParams.topMargin = dp(4); tile.addView(name, labelParams);
            }
            tile.setBackground(card());DisplayUi.apply(tile,displayPalette);
            LauncherAppCatalog.App app = getItem(position);
            String key = app.component.flattenToString();
            tile.setTag(key); tile.setContentDescription("Abrir " + app.label);
            ((TextView)tile.getChildAt(1)).setText(app.label);
            ImageView image = (ImageView)tile.getChildAt(0);
            image.setTag(key); image.setImageDrawable(null);
            Bitmap cached = icons.get(key);
            if (cached != null) image.setImageBitmap(cached);
            else requestIcon(app);
            return tile;
        }
    }

    private void requestIcon(LauncherAppCatalog.App app) {
        final String key = app.component.flattenToString();
        if (!active || !iconsPending.add(key)) return;
        final int request = generation;
        final int size = Math.min(128, dp(60));
        final Context appContext = getApplicationContext();
        worker.execute(() -> {
            Bitmap bitmap = null;
            try {
                Drawable original = appContext.getPackageManager().getActivityIcon(app.component);
                bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                original.setBounds(0, 0, size, size); original.draw(new Canvas(bitmap));
            } catch (PackageManager.NameNotFoundException | RuntimeException error) {
                Log.w(LauncherAppCatalog.TAG, "Icon unavailable: " + app.component, error);
            }
            final Bitmap result = bitmap;
            main.post(() -> {
                if (!active || generation != request) return;
                iconsPending.remove(key);
                if (result != null) icons.put(key, result);
                // Update only matching visible recycled cells, without rebinding the grid.
                for (int i = 0; i < grid.getChildCount(); i++) {
                    LinearLayout tile = (LinearLayout)grid.getChildAt(i);
                    ImageView image = (ImageView)tile.getChildAt(0);
                    if (key.equals(image.getTag())) {
                        if (result != null) image.setImageBitmap(result);
                        else image.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                }
            });
        });
    }
}
