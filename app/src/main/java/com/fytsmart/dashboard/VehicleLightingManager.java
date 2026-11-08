package com.fytsmart.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Controls virtual artwork only. No Binder, MCU, CAN transmissions or screen brightness writes. */
public final class VehicleLightingManager implements AutoCloseable {
    public interface Listener { void onLighting(VehicleLightingPolicy.State state); }
    private static final long CAN_TTL_MS=30_000;
    private final Context context;
    private final Listener listener;
    private final Clock clock;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private int onMinute=18*60,offMinute=6*60;
    private boolean active,authorized,verified;
    private String sourceId;
    private Boolean signal;
    private long signalAt;
    private VehicleLightingPolicy.State state;
    private final Runnable boundary=this::refresh;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent intent){refresh();}
    };
    public VehicleLightingManager(Context context,Listener listener){this(context,listener,Clock.systemUTC());}
    VehicleLightingManager(Context context,Listener listener,Clock clock) {
        this.context=context.getApplicationContext();this.listener=listener;this.clock=clock;
        try {
            android.content.SharedPreferences p=this.context.getSharedPreferences("vehicle_lighting",Context.MODE_PRIVATE);
            int on=p.getInt("on_minute",1080),off=p.getInt("off_minute",360);VehicleLightingPolicy.validate(on,off);onMinute=on;offMinute=off;
        }catch(RuntimeException ignored){onMinute=1080;offMinute=360;}
    }
    public void setSchedule(int on,int off) {
        VehicleLightingPolicy.validate(on,off);onMinute=on;offMinute=off;
        context.getSharedPreferences("vehicle_lighting",Context.MODE_PRIVATE).edit().putInt("on_minute",on).putInt("off_minute",off).apply();
        if(active)refresh();
    }
    public void start() {
        if(active)return;active=true;
        IntentFilter f=new IntentFilter();f.addAction(Intent.ACTION_TIME_CHANGED);f.addAction(Intent.ACTION_TIMEZONE_CHANGED);f.addAction(Intent.ACTION_DATE_CHANGED);
        context.registerReceiver(receiver,f);refresh();
    }
    public void stop() {if(active){active=false;context.unregisterReceiver(receiver);}handler.removeCallbacks(boundary);}
    /** Future adapter gate. Not called by production code: requires user authorization AND on-car validation. */
    public void configureVerifiedCanbus(String id,boolean userAuthorized,boolean validatedOnVehicle) {
        sourceId=id;authorized=userAuthorized&&id!=null&&!id.trim().isEmpty();verified=validatedOnVehicle;
        signal=null;if(active)refresh();
    }
    /** Future verified read-only event adapter only; no connection is established here. Main-thread API. */
    public void acceptVerifiedCanbusSample(String id,boolean lightsOn) {
        if(!authorized||!verified||id==null||!id.equals(sourceId))return;
        signal=lightsOn;signalAt=SystemClock.elapsedRealtime();if(active)refresh();
    }
    public VehicleLightingPolicy.State getState(){return state;}
    private void refresh() {
        if(!active)return;
        handler.removeCallbacks(boundary);
        ZonedDateTime now=ZonedDateTime.ofInstant(clock.instant(),ZoneId.systemDefault());
        long age=signal==null?-1:SystemClock.elapsedRealtime()-signalAt;
        VehicleLightingPolicy.State next=VehicleLightingPolicy.evaluate(now,onMinute,offMinute,authorized,verified,signal,age,CAN_TTL_MS);
        if(state==null||state.on!=next.on||state.source!=next.source){state=next;listener.onLighting(next);}
        long delay=Math.max(50,VehicleLightingPolicy.nextBoundary(now,onMinute,offMinute).toInstant().toEpochMilli()-clock.millis()+50);
        if(authorized&&verified&&signal!=null&&age>=0&&age<CAN_TTL_MS)delay=Math.min(delay,CAN_TTL_MS-age+1);
        handler.postDelayed(boundary,delay);
    }
    @Override public void close(){stop();}
}
