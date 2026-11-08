package com.fytsmart.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

/** Applies colors in place, without recreating activities, restarting GPS or touching audio. */
final class DisplayModeController implements AutoCloseable {
    private final Context context;
    private final DisplayPreferences preferences;
    private final Consumer<DashboardPalette> listener;
    private final Clock clock;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private DashboardPalette last;
    private final Runnable boundary=this::refresh;
    private final BroadcastReceiver receiver=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) { refresh(); }
    };
    DisplayModeController(Context context,Consumer<DashboardPalette> listener) {
        this(context,listener,Clock.systemUTC());
    }
    DisplayModeController(Context context,Consumer<DashboardPalette> listener,Clock clock) {
        this.context=context.getApplicationContext();preferences=new DisplayPreferences(this.context);
        this.listener=listener;this.clock=clock;
    }
    DashboardPalette current() { return DashboardPalette.forLight(preferences.load().isLight(now())); }
    private ZonedDateTime now() { return ZonedDateTime.ofInstant(clock.instant(),ZoneId.systemDefault()); }
    void start() {
        if(active)return;
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);filter.addAction(Intent.ACTION_DATE_CHANGED);
        context.registerReceiver(receiver,filter);active=true;last=null;refresh();
    }
    void refresh() {
        if(!active)return;
        handler.removeCallbacks(boundary);
        DisplayMode mode=preferences.load();ZonedDateTime now=now();
        DashboardPalette next=DashboardPalette.forLight(mode.isLight(now));
        if(next!=last) { last=next;listener.accept(next); }
        if(mode==DisplayMode.AUTO) {
            long delay=Math.max(50,DisplayMode.nextBoundary(now).toInstant().toEpochMilli()-clock.millis()+25);
            handler.postDelayed(boundary,delay);
        }
    }
    void stop() {
        if(active) { active=false;context.unregisterReceiver(receiver); }
        handler.removeCallbacks(boundary);
    }
    @Override public void close() { stop(); }
}
