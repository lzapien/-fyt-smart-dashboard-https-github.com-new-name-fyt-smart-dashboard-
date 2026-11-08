package com.fytsmart.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Spanish date refreshed on resume, date/time-zone changes or the next local midnight. */
final class SpanishDateView extends TextView implements AutoCloseable {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private boolean active;
    private final Runnable midnight=this::refresh;
    private final BroadcastReceiver receiver=new BroadcastReceiver(){
        @Override public void onReceive(Context c,Intent i){refresh();}
    };
    SpanishDateView(Context context){super(context);setSingleLine(true);setText(SpanishDateFormatter.format(Instant.now(),ZoneId.systemDefault()));}
    void start(){
        if(active)return;active=true;
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_DATE_CHANGED);filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(receiver,filter);refresh();
    }
    void stop(){if(active){active=false;getContext().unregisterReceiver(receiver);}handler.removeCallbacks(midnight);}
    private void refresh(){
        if(!active)return;
        handler.removeCallbacks(midnight);ZoneId zone=ZoneId.systemDefault();Instant now=Instant.now();
        setText(SpanishDateFormatter.format(now,zone));
        ZonedDateTime next=now.atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone);
        handler.postDelayed(midnight,Math.max(50,next.toInstant().toEpochMilli()-now.toEpochMilli()+50));
    }
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override public void close(){stop();}
}
