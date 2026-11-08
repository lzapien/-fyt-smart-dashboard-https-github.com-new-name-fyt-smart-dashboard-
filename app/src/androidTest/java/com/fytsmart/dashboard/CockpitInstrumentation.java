package com.fytsmart.dashboard;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Explicit fixtures confined to the separate test APK. Never included in production UI/data. */
public final class CockpitInstrumentation extends Instrumentation {
    private int passed;
    private Activity activity;
    private MediaSession session;
    private boolean drawerSuite;
    private boolean appearanceSuite;
    private final StringBuilder lightingEvidence=new StringBuilder();
    @Override public void onCreate(Bundle args) {
        super.onCreate(args);
        drawerSuite=args!=null && "drawer".equals(args.getString("suite"));
        appearanceSuite=args!=null && "appearance".equals(args.getString("suite"));
        start();
    }
    private Object field(String name) throws Exception {
        Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);
    }
    private void set(String name,Object value) throws Exception {
        Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);
    }
    private void check(boolean valid,String name) { if(!valid)throw new AssertionError(name);passed++; }
    private void ui(Runnable task) { runOnMainSync(task); }
    private String text(String name) throws Exception { return ((TextView)field(name)).getText().toString(); }
    @Override public void onStart() {
        if(appearanceSuite){new AppearanceChecks(this).run();return;}
        if(drawerSuite){new AppDrawerChecks(this).run();return;}
        Bundle result=new Bundle();
        try {
            activity=startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            ui(()->{
                try {
                    ((android.os.Handler)field("handler")).removeCallbacks((Runnable)field("ticker"));
                    ((MainActivity)activity).disconnectGps();
                    String expectedDate=SpanishDateFormatter.format(java.time.Instant.now(),java.time.ZoneId.systemDefault());
                    check(text("dateLabel").equals(expectedDate),"Date is es-MX with Android local calendar");
                    check(text("lightingStatus").isEmpty(),"Lighting caption removed without disabling manager");
                    android.content.pm.ActivityInfo homeInfo=getTargetContext().getPackageManager().getActivityInfo(new android.content.ComponentName(getTargetContext(),MainActivity.class),0);
                    check(homeInfo.launchMode==android.content.pm.ActivityInfo.LAUNCH_SINGLE_TASK,"HOME singleTask prevents duplicate instances");
                    Intent homeQuery=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addCategory(Intent.CATEGORY_DEFAULT).setPackage(getTargetContext().getPackageName());
                    check(getTargetContext().getPackageManager().resolveActivity(homeQuery,0)!=null,"HOME MAIN/DEFAULT resolves");
                    check(getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName())!=null,"Normal launcher entry retained");
                    android.widget.LinearLayout dock=activity.findViewById(android.R.id.content).findViewWithTag("custom_dock");
                    check(dock!=null && dock.getChildCount()==5,"Exactly five dock cards");
                    String[] dockPackages={"com.navimods.radio","com.waze","com.google.android.apps.maps","com.amazon.mp3","com.google.android.apps.youtube.music"};
                    String[] dockNames={"Radio","Waze","Google Maps","Amazon Music","YouTube Music"};
                    int width=dock.getChildAt(0).getWidth();
                    check(width>=48*getTargetContext().getResources().getDisplayMetrics().density,"Dock touch targets at least 48 dp wide");
                    for(int i=0;i<5;i++) {
                        android.widget.LinearLayout tile=(android.widget.LinearLayout)dock.getChildAt(i);
                        check(dockPackages[i].equals(tile.getTag()),"Dock package/order "+i);
                        check(dockNames[i].equals(((TextView)tile.getChildAt(1)).getText().toString()),"Dock label "+i);
                        check(((ImageView)tile.getChildAt(0)).getDrawable() instanceof DockIconDrawable,"Custom vector icon "+i);
                        check(Math.abs(tile.getWidth()-width)<=1,"Uniform dock width "+i);
                    }
                    Bitmap pin=Bitmap.createBitmap(128,128,Bitmap.Config.ARGB_8888);
                    DockIconDrawable pinIcon=new DockIconDrawable("maps",0xff5bcfff);pinIcon.setBounds(0,0,128,128);
                    pinIcon.draw(new android.graphics.Canvas(pin));
                    boolean blue=false,red=false,amber=false,green=false;
                    for(int y=0;y<128;y++)for(int x=0;x<128;x++) {
                        int pixel=pin.getPixel(x,y);
                        if(Color.alpha(pixel)<220)continue;
                        blue|=Color.blue(pixel)>220 && Color.red(pixel)<130;
                        red|=Color.red(pixel)>230 && Color.green(pixel)<140;
                        amber|=Color.red(pixel)>230 && Color.green(pixel)>160 && Color.blue(pixel)<150;
                        green|=Color.green(pixel)>190 && Color.red(pixel)<130 && Color.blue(pixel)<220;
                    }
                    check(blue&&red&&amber&&green,"Custom Maps pin has multicolor accents");
                    SpeedometerView gauge=(SpeedometerView)field("speedometer");
                    SuvRearView car=(SuvRearView)field("suvView");
                    String priorReading=gauge.getDisplayValue();
                    ((android.location.LocationListener)activity).onStatusChanged("gps",2,new Bundle());
                    check(priorReading.equals(gauge.getDisplayValue()),"API 29 status callback cannot invent a speed");
                    android.view.View clock=(android.view.View)field("mainClock");
                    int[] gaugeAt=new int[2],carAt=new int[2],clockAt=new int[2];
                    gauge.getLocationOnScreen(gaugeAt);car.getLocationOnScreen(carAt);clock.getLocationOnScreen(clockAt);
                    check(gauge.getWidth()>0&&gauge.getHeight()>0,"Real Canvas speedometer has visible bounds");
                    check(carAt[1]>=gaugeAt[1]+gauge.getHeight(),"Rear SUV below dial");
                    check(clockAt[0]>gaugeAt[0]+gauge.getWidth(),"Clock in right column");
                    android.view.View weatherCard=activity.findViewById(android.R.id.content).findViewWithTag("weather_panel");
                    android.view.View mediaCard=activity.findViewById(android.R.id.content).findViewWithTag("media_panel");
                    int[] weatherAt=new int[2],mediaAt=new int[2];weatherCard.getLocationOnScreen(weatherAt);mediaCard.getLocationOnScreen(mediaAt);
                    check(weatherAt[1]>=clockAt[1]+clock.getHeight(),"Weather below clock");
                    check(mediaAt[1]>=weatherAt[1]+weatherCard.getHeight(),"Media below weather");
                    android.view.View root=activity.findViewById(android.R.id.content).findViewWithTag("cockpit_root");
                    check(((android.graphics.drawable.ColorDrawable)root.getBackground()).getColor()==Color.BLACK,"Background exactly #000000");
                    SpeedometerView drawn=new SpeedometerView(getTargetContext());
                    drawn.layout(0,0,440,290);drawn.setGpsSpeed(110,false);
                    Bitmap dial=Bitmap.createBitmap(440,290,Bitmap.Config.ARGB_8888);drawn.draw(new android.graphics.Canvas(dial));
                    int bluePixels=0;
                    for(int y=0;y<290;y++)for(int x=0;x<440;x++) {
                        int pixel=dial.getPixel(x,y);if(Color.alpha(pixel)>100&&Color.blue(pixel)>150&&Color.blue(pixel)>Color.red(pixel)*1.3f)bluePixels++;
                    }
                    check(bluePixels>500,"Dial renders blue graphical arc, not only text");
                    check(car.getWidth()>0&&car.getHeight()>0,"Generic vehicle view has visible bounds");
                    set("weatherLoading",true); // Fixtures must not initiate a network request.
                    GpsIndicatorView indicator=(GpsIndicatorView)field("gpsIndicator");
                    int indicatorWidth=indicator.getWidth(),indicatorHeight=indicator.getHeight();
                    float[] meters={5,10,15,25,30};int[] colors={GpsTelemetry.GREEN,GpsTelemetry.GREEN,GpsTelemetry.YELLOW,GpsTelemetry.YELLOW,GpsTelemetry.RED};
                    for(int i=0;i<meters.length;i++) {
                        Location fix=new Location("gps");fix.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());fix.setAccuracy(meters[i]);fix.setSpeed(0);fix.setBearing(90);
                        ((MainActivity)activity).onLocationChanged(fix);check(indicator.getIndicatorColor()==colors[i],"GPS horizontal accuracy color "+meters[i]);
                    }
                    check(indicator.getWidth()==indicatorWidth&&indicator.getHeight()==indicatorHeight,"GPS icon fixed dimensions across colors");
                    check(text("compassStatus").equals("--"),"Stationary GPS does not invent heading");
                    Location east=new Location("gps");east.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());east.setSpeed(5);east.setAccuracy(5);east.setBearing(90);
                    ((MainActivity)activity).onLocationChanged(east);check(text("compassStatus").equals("E"),"GPS course compass E");
                    check(gauge.getContentDescription().toString().contains("0 a 120"),"Dial scale is 120 not 220");
                    for(int kmh:new int[]{0,25,65,95}) {
                        Location location=new Location("gps");location.setLatitude(19.43);location.setLongitude(-99.13);location.setAccuracy(5);location.setSpeed(kmh/3.6f);location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        ((MainActivity)activity).onLocationChanged(location);
                        check(((SpeedometerView)field("speedometer")).getDisplayValue().equals(Integer.toString(kmh)),"GPS digital dial speed "+kmh);
                    }
                    if(android.animation.ValueAnimator.areAnimatorsEnabled()) {
                        car.setRunning(true,true);car.setGpsSpeed(60);
                        Field motionField=SuvRearView.class.getDeclaredField("motion");motionField.setAccessible(true);
                        check(motionField.get(car)!=null && ((android.animation.ValueAnimator)motionField.get(car)).isRunning(),"SUV road animation runs with GPS motion");
                        car.setGpsSpeed(0);check(motionField.get(car)==null,"SUV road animation stops at zero GPS speed");
                    }
                    Method weather=MainActivity.class.getDeclaredMethod("renderWeather",String.class,boolean.class);weather.setAccessible(true);
                    set("weatherWallTime",System.currentTimeMillis());
                    weather.invoke(activity,"☾  -3 °C  ·  Despejado",false);
                    check(text("weatherTemperature").equals("-3°C"),"Negative temperature");
                    check(text("weatherCondition").equals("Despejado"),"Weather condition");
                    check(activity.getWindow().getDecorView().findViewWithTag("exterior_temperature")==null,"Unverified exterior temperature indicator is removed");
                    Method controls=MainActivity.class.getDeclaredMethod("setControls",PlaybackState.class);controls.setAccessible(true);
                    controls.invoke(activity,new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING,0,1)
                            .setActions(PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT).build());
                    check(((Button)field("playButton")).isEnabled(),"Pause action enabled");
                    check(!((Button)field("previousButton")).isEnabled(),"Unsupported previous disabled");
                    check(((Button)field("nextButton")).isEnabled(),"Next action enabled");
                    session=new MediaSession(getTargetContext(),"EXPLICIT_UI_TEST_FIXTURE");
                    Bitmap art=Bitmap.createBitmap(1200,1200,Bitmap.Config.ARGB_8888);art.eraseColor(Color.rgb(30,140,170));
                    session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,"PRUEBA UI — no es una canción real")
                            .putString(MediaMetadata.METADATA_KEY_ARTIST,"Fixture solo del APK de pruebas").putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,art).build());
                    session.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PAUSED,0,0)
                            .setActions(PlaybackState.ACTION_PLAY).build());
                    Method show=MainActivity.class.getDeclaredMethod("showMedia",MediaController.class);show.setAccessible(true);
                    show.invoke(activity,new MediaController(getTargetContext(),session.getSessionToken()));
                    check(text("track").startsWith("PRUEBA UI"),"Published title rendered");
                    check(text("artistLabel").equals("Fixture solo del APK de pruebas"),"Published artist rendered");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            final boolean[] loaded={false};
            for(int i=0;i<30&&!loaded[0];i++) {
                SystemClock.sleep(100);
                ui(()->{try{loaded[0]=((ImageView)field("albumArt")).getDrawable()!=null;}catch(Exception e){throw new RuntimeException(e);}});
            }
            check(loaded[0],"Artwork loaded asynchronously");
            ui(()->{
                try {
                    android.graphics.drawable.BitmapDrawable cover=(android.graphics.drawable.BitmapDrawable)((ImageView)field("albumArt")).getDrawable();
                    check(Math.max(cover.getBitmap().getWidth(),cover.getBitmap().getHeight())<=384,"Artwork bounded to 384 pixels");
                    SuvRearView vehicle=(SuvRearView)field("suvView");
                    check(vehicle.hasDetailedArtwork(),"Generic Canvas vehicle is available");
                    int[] minuteCases={359,360,1079,1080};boolean[] expectedLight={true,false,false,true};
                    for(int i=0;i<minuteCases.length;i++) {
                        int minute=minuteCases[i];java.time.ZoneId zone=java.time.ZoneId.systemDefault();
                        java.time.Instant instant=java.time.LocalDate.of(2026,10,29).atTime(minute/60,minute%60).atZone(zone).toInstant();
                        VehicleLightingManager manager=new VehicleLightingManager(getTargetContext(),state->vehicle.setLighting(state,false),java.time.Clock.fixed(instant,java.time.ZoneOffset.UTC));
                        manager.start();VehicleLightingPolicy.State light=manager.getState();
                        check(light.on==expectedLight[i],"Lighting boundary "+minute);
                        check(light.source==VehicleLightingPolicy.Source.SCHEDULE_DECORATIVE,"Schedule never reported as CANBUS "+minute);
                        check(vehicle.getLightingLevel()==(expectedLight[i]?1:0),"Independent graphical layers "+minute);
                        lightingEvidence.append(String.format(java.util.Locale.ROOT,"%02d:%02d -> %s / %s\n",minute/60,minute%60,light.on?"ON":"OFF",light.source));manager.close();
                    }
                    Method clear=MainActivity.class.getDeclaredMethod("clearMedia",String.class);clear.setAccessible(true);clear.invoke(activity,"Prueba terminada");
                    check(((ImageView)field("albumArt")).getDrawable()==null,"Artwork cleared without session");
                    check(((android.view.View)field("artworkFallback")).getVisibility()==android.view.View.VISIBLE,"Fallback icon visible");
                    check(!((Button)field("playButton")).isEnabled(),"No-session controls disabled");
                    check(text("track").isEmpty()&&text("artistLabel").isEmpty(),"Empty media has no technical title");
                    check(text("playbackLabel").isEmpty()&&text("mediaStatus").isEmpty(),"Empty media has no absence messages");
                    check(((android.view.View)field("artworkFallback")).getVisibility()==android.view.View.VISIBLE,"Discreet generic icon retained");
                    Method save=MainActivity.class.getDeclaredMethod("onSaveInstanceState",Bundle.class);save.setAccessible(true);
                    Bundle state=new Bundle();set("lastWeatherDisplay","☾  -3 °C  ·  Despejado");
                    // An actual pending weather callback may have completed while artwork loaded.
                    // Establish the in-flight fixture at the point being tested, on the same UI turn.
                    set("weatherLoading",true);save.invoke(activity,state);
                    check(state.getString("weather_display").contains("-3"),"Weather saved for recreation");
                    check(state.getLong("weather_attempt")==0,"In-flight request can retry after recreation");
                    Location saved=state.getParcelable("gps_fix");
                    check(saved!=null && Math.round(saved.getSpeed()*3.6f)==95,"Timestamped GPS fix saved for recreation");
                    SpeedometerView gauge=(SpeedometerView)field("speedometer");
                    Location noSpeed=new Location("gps");noSpeed.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    ((MainActivity)activity).onLocationChanged(noSpeed);check(gauge.getDisplayValue().equals("--"),"Missing speed is unknown, not zero");
                    Location stale=new Location("gps");stale.setSpeed(10);stale.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos()-16_000_000_000L);
                    ((MainActivity)activity).onLocationChanged(stale);check(gauge.getDisplayValue().equals("--"),"Stale GPS is not displayed");
                    ((MainActivity)activity).onProviderDisabled("gps");check(gauge.getDisplayValue().equals("--"),"Disabled GPS is visible as unknown");
                    check(((GpsIndicatorView)field("gpsIndicator")).getIndicatorColor()==GpsTelemetry.RED,"Lost GPS turns icon red");
                    check(text("compassStatus").equals("--"),"Lost GPS removes compass heading");
                    gauge.setGpsSpeed(235,false);check(gauge.getDisplayValue().equals("235")&&gauge.getArcSpeed()==120,"Digital GPS value not falsified above dial limit");
                    gauge.setRunning(true);gauge.setGpsSpeed(0,false);gauge.setGpsSpeed(100,true);
                    check(gauge.getDisplayValue().equals("100"),"Digital value reports actual GPS during interpolation");
                    if(android.animation.ValueAnimator.areAnimatorsEnabled())check(gauge.getArcSpeed()<100,"Arc interpolates using ValueAnimator");
                }catch(Exception e){throw new RuntimeException(e);}
            });
            SystemClock.sleep(650);
            ui(()->{try{check(Math.abs(((SpeedometerView)field("speedometer")).getArcSpeed()-100)<.01f,"Animated arc reaches GPS target");}catch(Exception e){throw new RuntimeException(e);}});
            for(int minute:new int[]{360,1080}) {
                final VehicleLightingManager[] manager={null};
                ui(()->{
                    try {
                        java.time.ZoneId zone=java.time.ZoneId.systemDefault();
                        java.time.Instant at=java.time.LocalDate.of(2026,10,29).atTime(minute/60,0).atZone(zone).toInstant();
                        ((SpeedometerView)field("speedometer")).setUnknown("PRUEBA DE HORARIO");
                        ((TextView)field("lightingStatus")).setText("PRUEBA HORARIO "+(minute/60)+":00 · decorativo, no CANBUS");
                        manager[0]=new VehicleLightingManager(getTargetContext(),state->((SuvRearView)activity.findViewById(android.R.id.content).findViewWithTag("rear_suv")).setLighting(state,false),java.time.Clock.fixed(at,java.time.ZoneOffset.UTC));
                        manager[0].start();
                    }catch(Exception e){throw new RuntimeException(e);}
                });
                waitForIdleSync();SystemClock.sleep(250);
                Bitmap screenshot=getUiAutomation().takeScreenshot();
                if(screenshot!=null)try(java.io.FileOutputStream output=new java.io.FileOutputStream(new java.io.File(getTargetContext().getFilesDir(),"lighting-test-"+minute+".png"))) {
                    screenshot.compress(Bitmap.CompressFormat.PNG,100,output);
                }
                ui(()->manager[0].close());
            }
            result.putString("stream","Cockpit API "+android.os.Build.VERSION.SDK_INT+": "+passed+" checks passed\nFixtures were test-only.\n"+lightingEvidence+"");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable error) {
            result.putString("stream","FAILED after "+passed+" checks: "+error+"\n");finish(Activity.RESULT_CANCELED,result);
        } finally {
            if(session!=null)session.release();
        }
    }
}
