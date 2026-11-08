package com.fytsmart.dashboard;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.os.PowerManager;
import android.text.format.DateFormat;
import android.widget.FrameLayout;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ScrollView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.widget.TextClock;
import android.widget.ImageView;

public class MainActivity extends Activity implements LocationListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private DashboardPalette displayPalette=DashboardPalette.DARK;
    private DisplayModeController displayController;
    private final java.util.List<java.util.function.Consumer<DashboardPalette>> paletteBindings=new java.util.ArrayList<>();
    private Integer mediaTint;
    private void bindPalette(java.util.function.Consumer<DashboardPalette> binding) {
        paletteBindings.add(binding);binding.accept(displayPalette);
    }
    private void applyDisplayPalette(DashboardPalette palette) {
        displayPalette=palette;
        for(java.util.function.Consumer<DashboardPalette> binding:paletteBindings)binding.accept(palette);
        speedometer.setPalette(palette);suvView.setPalette(palette);compassIndicator.setPalette(palette);
        gpsIndicator.setPalette(palette);weatherScene.setPalette(palette);updateMediaBackground();
    }
    private GradientDrawable themedGradient(int first,int second,float radius) {
        return gradient(displayPalette.light?displayPalette.panelTop:first,displayPalette.light?displayPalette.panelBottom:second,radius);
    }
    private void updateMediaBackground() {
        if(mediaPanel==null)return;
        if(mediaTint==null||displayPalette.light)mediaPanel.setBackground(themedGradient(0xff0d151f,0xff070c12,22));
        else {
            int tint=mediaTint;
            int dark=Color.rgb(Color.red(tint)/5+12,Color.green(tint)/5+18,Color.blue(tint)/5+25);
            mediaPanel.setBackground(gradient(dark,0xff111e2c,22));
        }
    }

    private TextView track, mediaStatus, artistLabel, playbackLabel;
    private TextView weatherTemperature, weatherCondition, weatherIcon, weatherUpdated, gpsStatus;
    private TextView compassStatus,lightingStatus;
    private GpsIndicatorView gpsIndicator;
    private CompassIndicatorView compassIndicator;
    private VehicleLightingManager lightingManager;
    private float gpsGreenMeters=10,gpsYellowMeters=25;
    private TextClock mainClock;
    private SpanishDateView dateLabel;
    private ImageView albumArt;
    private View artworkFallback;
    private FrameLayout artworkCover;
    private LinearLayout mediaPanel;
    private Button previousButton, playButton, nextButton, permissionsButton;
    private WeatherScene weatherScene;
    private MediaArtwork artworkLoader;
    private String mediaKey = "";
    private String lastWeatherDisplay;
    private long weatherWallTime;
    private boolean resumed;
    private boolean destroyed;
    private boolean compact;
    private ValueAnimator playbackPulse;
    private SpeedometerView speedometer;
    private SuvRearView suvView;
    private ThemeManager appearanceManager;
    private Location lastGpsLocation;
    private String gpsUnavailableReason="Esperando señal GPS";
    private MediaController currentController;
    private GpsLocationService gpsService;
    private boolean gpsBound, locationPermissionRequest;
    private final GpsLocationService.Listener gpsListener = new GpsLocationService.Listener() {
        @Override public void onGpsLocation(Location location) {
            if(resumed&&!destroyed)onLocationChanged(location);
        }
        @Override public void onGpsStatus(GpsLocationService.Status status) {
            if(!resumed||destroyed)return;
            gpsUnavailableReason=status.description;
            if(status==GpsLocationService.Status.DISABLED||status==GpsLocationService.Status.NO_PERMISSION
                    ||status==GpsLocationService.Status.UNAVAILABLE)lastGpsLocation=null;
            refreshSpeedValidity();
        }
    };
    private final ServiceConnection gpsConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name,IBinder binder) {
            if(!gpsBound||!resumed||destroyed){disconnectGps();return;}
            gpsService=((GpsLocationService.LocalBinder)binder).service();gpsService.addListener(gpsListener);
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            gpsService=null;lastGpsLocation=null;if(!destroyed)setGpsUnknown("Servicio GPS desconectado");
        }
        @Override public void onBindingDied(ComponentName name) {disconnectGps();if(!destroyed)setGpsUnknown("Servicio GPS no disponible");}
    };
    private SourceManager sourceManager;
    private MediaSessionCoordinator mediaCoordinator;
    private String displayedMediaPackage;
    private long artworkRevision;
    private ImageButton weatherRefreshButton;
    private long lastManualWeatherRefresh;

    // Control de actualizaciones meteorológicas
    private Location lastWeatherLocation;
    private long lastWeatherSuccess = 0;
    private long lastWeatherAttempt = 0;
    private boolean weatherLoading = false;

    private void updateWeather(Location location) { updateWeather(location,false); }
    private void updateWeather(Location location, boolean manual) {
        if (location == null || weatherLoading) return;

        long now = android.os.SystemClock.elapsedRealtime();

        // Evitar consultas repetidas si falla Internet
        if (!manual && lastWeatherAttempt > 0 &&
                now - lastWeatherAttempt < 5 * 60 * 1000L) {
            return;
        }

        boolean first = lastWeatherLocation == null;

        boolean moved = !first &&
                location.distanceTo(lastWeatherLocation) >= 5000;

        // Actualizar cada 20 minutos o al cambiar de zona
        if (!manual && !first && !moved &&
                now - lastWeatherSuccess < 20 * 60 * 1000L) {
            return;
        }

        weatherLoading = true;
        if(weatherRefreshButton!=null)weatherRefreshButton.setEnabled(false);
        lastWeatherAttempt = now;

        WeatherClient.fetch(
                location.getLatitude(),
                location.getLongitude(),
                new WeatherClient.Callback() {
                    @Override
                    public void onResult(String result) {
                        if (destroyed) return;
                        weatherLoading = false;
                        if(weatherRefreshButton!=null)weatherRefreshButton.setEnabled(true);
                        lastWeatherLocation = new Location(location);
                        lastWeatherSuccess =
                                android.os.SystemClock.elapsedRealtime();

                        weatherWallTime = System.currentTimeMillis();
                        lastWeatherDisplay = result;
                        renderWeather(result, true);
                    }

                    @Override
                    public void onError() {
                        if (destroyed) return;
                        weatherLoading = false;
                        if(weatherRefreshButton!=null)weatherRefreshButton.setEnabled(true);

                        if (lastWeatherLocation == null) {
                            weatherCondition.setText("Clima no disponible");
                            weatherUpdated.setText("Se reintentará con ubicación y conexión");
                        }
                    }
                }
        );
    }
    private void refreshWeatherManually() {
        long now=android.os.SystemClock.elapsedRealtime();
        if(weatherLoading || (lastManualWeatherRefresh>0&&now-lastManualWeatherRefresh<10_000))return;
        Location fix=lastGpsLocation;
        long nanos=android.os.SystemClock.elapsedRealtimeNanos();
        if(fix==null||fix.getElapsedRealtimeNanos()<=0||nanos<fix.getElapsedRealtimeNanos()
                ||nanos-fix.getElapsedRealtimeNanos()>GpsSpeedState.MAX_AGE_NANOS) {
            Toast.makeText(this,"Esperando ubicación GPS para actualizar el clima",Toast.LENGTH_SHORT).show();return;
        }
        lastManualWeatherRefresh=now;updateWeather(new Location(fix),true);
    }
    private static final int BG=0xff101827, PANEL=0xff202c3e, WHITE=0xfff6f8fc, SECONDARY=0xffa9bbd0, ACCENT=0xff37b8db;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refreshSpeedValidity();
            handler.postDelayed(this, 2500);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        applyImmersiveMode();
        artworkLoader = new MediaArtwork(getApplicationContext());
        sourceManager = new SourceManager(getApplicationContext(),
                message -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
        appearanceManager=new ThemeManager(getApplicationContext());
        displayController=new DisplayModeController(this,this::applyDisplayPalette);
        displayPalette=displayController.current();
        createUi();suvView.applyAppearance(appearanceManager.load());
        mediaCoordinator=new MediaSessionCoordinator(this,new MediaSessionCoordinator.Listener(){
            @Override public void onMedia(MediaController controller,MediaMetadata metadata,PlaybackState state,String pkg,boolean dirty){
                if(!destroyed)try { renderMedia(controller,metadata,state,pkg,dirty); }
                catch(RuntimeException invalidMetadata) {
                    android.util.Log.w("FYT_Media","Invalid or unavailable session data",invalidMetadata);clearMedia("");
                }
            }
            @Override public void onAccessRequired(boolean required){
                if(!destroyed)permissionsButton.setVisibility(required?View.VISIBLE:View.GONE);
            }
        });
        lightingManager=new VehicleLightingManager(getApplicationContext(),lighting->{
            suvView.setLighting(lighting,resumed&&animationsAllowed());
            lightingStatus.setText("");
        });
        try {
            android.content.SharedPreferences gpsPrefs=getSharedPreferences("cockpit_gps",MODE_PRIVATE);
            gpsGreenMeters=gpsPrefs.getFloat("green_m",10);gpsYellowMeters=gpsPrefs.getFloat("yellow_m",25);
            if(!(Float.isFinite(gpsGreenMeters)&&Float.isFinite(gpsYellowMeters)&&gpsGreenMeters>0&&gpsYellowMeters>gpsGreenMeters)){gpsGreenMeters=10;gpsYellowMeters=25;}
        }catch(RuntimeException invalid){gpsGreenMeters=10;gpsYellowMeters=25;}
        if (state != null) {
            lastWeatherDisplay = state.getString("weather_display");
            weatherWallTime = state.getLong("weather_time");
            lastWeatherSuccess = state.getLong("weather_success");
            lastWeatherAttempt = state.getLong("weather_attempt");
            lastWeatherLocation = state.getParcelable("weather_location");
            if (lastWeatherDisplay != null) renderWeather(lastWeatherDisplay, false);
            lastGpsLocation=state.getParcelable("gps_fix");
            refreshSpeedValidity();
        }
        askLocationAndStart();
    }
    private void applyImmersiveMode() {
        getWindow().setFlags(android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);if(focused)applyImmersiveMode();
    }
    private int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color, float radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;
    }
    private GradientDrawable gradient(int first,int second,float radius) {
        GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{first,second});
        d.setCornerRadius(dp(radius));d.setStroke(dp(1.5f),displayPalette.border);return d;
    }
    private TextView label(String value,int sp,int color,boolean bold) { return label(value,sp,color,bold,true); }
    private TextView label(String value,int sp,int color,boolean bold,boolean adaptive) {
        TextView v=new TextView(this);v.setText(value);v.setTextSize(sp);v.setTextColor(color);
        if(adaptive)bindPalette(p->v.setTextColor(p.textColor(color)));
        v.setGravity(Gravity.CENTER_VERTICAL);v.setIncludeFontPadding(false);
        v.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));return v;
    }
    private TextView caption(String value) {
        TextView v=label(value,compact?10:11,SECONDARY,true);v.setLetterSpacing(.16f);return v;
    }
    private LinearLayout vertical() { LinearLayout x=new LinearLayout(this);x.setOrientation(LinearLayout.VERTICAL);return x; }
    private LinearLayout.LayoutParams lp(int w,int h,int weight) { return new LinearLayout.LayoutParams(w,h,weight); }
    private LinearLayout panel() {
        LinearLayout p=vertical();p.setPadding(dp(compact?16:22),dp(compact?12:18),dp(compact?16:22),dp(compact?12:18));
        bindPalette(colors->p.setBackground(themedGradient(0xff0d151f,0xff070c12,22)));p.setElevation(dp(3));return p;
    }
    private void createUi() {
        compact=getResources().getConfiguration().screenHeightDp<500;
        int gap=dp(compact?8:12);
        LinearLayout root=vertical();root.setTag("cockpit_root");
        root.setPadding(dp(compact?14:24),dp(compact?10:16),dp(compact?14:24),dp(compact?10:16));
        bindPalette(p->root.setBackgroundColor(p.background));setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);
        permissionsButton=actionButton("ACCESO MULTIMEDIA");permissionsButton.setTextSize(10);
        permissionsButton.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        header.addView(permissionsButton,lp(dp(compact?138:160),dp(48),0));
        Button source=actionButton("SOURCE");source.setTag("source_button");bindPalette(p->source.setTextColor(p.accent));source.setContentDescription("Abrir aplicaciones; mantener pulsado para ajustes del dashboard");
        LinearLayout.LayoutParams sourceParams=lp(dp(108),dp(48),0);sourceParams.leftMargin=gap;header.addView(source,sourceParams);
        source.setOnClickListener(v->{
            DashboardRecoveryController.get(this).userInteraction();
            try { startActivity(new Intent(this,AppDrawerActivity.class)); }
            catch(RuntimeException e) {
                android.util.Log.w("FYT_AppDrawer","Cannot open app drawer",e);
                Toast.makeText(this,"Aplicaciones no disponibles",Toast.LENGTH_SHORT).show();
            }
        });
        source.setOnLongClickListener(v->{
            DashboardRecoveryController.get(this).userInteraction();
            startActivity(new Intent(this,DashboardSettingsActivity.class));return true;
        });

        LinearLayout center=new LinearLayout(this);
        LinearLayout left=panel();left.setTag("instruments_panel");
        left.setPadding(dp(12),dp(10),dp(12),dp(10));bindPalette(p->left.setBackground(themedGradient(0xff07111d,0xff02060b,22)));
        speedometer=new SpeedometerView(this);speedometer.setTag("speedometer");left.addView(speedometer,lp(-1,0,1));
        suvView=new SuvRearView(this);suvView.setTag("rear_suv");left.addView(suvView,lp(-1,dp(compact?120:220),0));
        lightingStatus=label("",compact?8:9,SECONDARY,false);lightingStatus.setGravity(Gravity.CENTER);left.addView(lightingStatus,lp(-1,dp(14),0));
        LinearLayout telemetry=new LinearLayout(this);telemetry.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout gpsGroup=new LinearLayout(this);gpsGroup.setGravity(Gravity.CENTER_VERTICAL);
        gpsIndicator=new GpsIndicatorView(this);gpsIndicator.setTag("gps_indicator");gpsGroup.addView(gpsIndicator,lp(dp(18),dp(18),0));
        gpsStatus=label("GPS -- m",compact?10:12,SECONDARY,false);gpsStatus.setPadding(dp(6),0,0,0);gpsGroup.addView(gpsStatus);
        telemetry.addView(gpsGroup,lp(0,-1,1));
        LinearLayout compassGroup=new LinearLayout(this);compassGroup.setGravity(Gravity.CENTER);
        compassIndicator=new CompassIndicatorView(this);compassGroup.addView(compassIndicator,lp(dp(18),dp(18),0));
        compassStatus=label("--",compact?10:12,SECONDARY,false);compassStatus.setTag("gps_compass");compassStatus.setPadding(dp(6),0,0,0);compassGroup.addView(compassStatus);
        telemetry.addView(compassGroup,lp(0,-1,1));
        left.addView(telemetry,lp(-1,dp(compact?28:32),0));
        LinearLayout.LayoutParams leftParams=lp(0,-1,47);leftParams.rightMargin=gap;center.addView(left,leftParams);

        LinearLayout right=vertical();right.setTag("right_column");
        LinearLayout clockCard=panel();clockCard.setTag("clock_panel");clockCard.setGravity(Gravity.CENTER_VERTICAL);
        clockCard.setPadding(dp(20),dp(compact?6:10),dp(20),dp(compact?6:10));
        mainClock=new TextClock(this);mainClock.setTag("main_clock");
        android.text.SpannableString twelveHour=new android.text.SpannableString("h:mm a");
        twelveHour.setSpan(new android.text.style.RelativeSizeSpan(.28f),5,6,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mainClock.setFormat12Hour(twelveHour);mainClock.setFormat24Hour("HH:mm");
        bindPalette(p->mainClock.setTextColor(p.text));mainClock.setTextSize(compact?54:88);
        mainClock.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));mainClock.setSingleLine();mainClock.setIncludeFontPadding(false);
        mainClock.setAutoSizeTextTypeUniformWithConfiguration(36,compact?54:88,2,android.util.TypedValue.COMPLEX_UNIT_SP);
        header.addView(mainClock,0,lp(0,-1,1));clockCard.addView(header,lp(-1,0,1));
        dateLabel=new SpanishDateView(this);dateLabel.setTag("spanish_date");bindPalette(p->dateLabel.setTextColor(p.secondary));dateLabel.setTextSize(compact?12:16);
        clockCard.addView(dateLabel,lp(-1,dp(compact?18:24),0));right.addView(clockCard,lp(-1,dp(compact?92:148),0));

        FrameLayout weatherCard=new FrameLayout(this);weatherCard.setTag("weather_panel");
        bindPalette(p->weatherCard.setBackground(themedGradient(0xff101d28,0xff080f17,18)));weatherCard.setClipToOutline(true);
        weatherScene=new WeatherScene(this);weatherCard.addView(weatherScene,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout weatherRow=new LinearLayout(this);weatherRow.setGravity(Gravity.CENTER_VERTICAL);weatherRow.setPadding(dp(12),dp(4),dp(12),dp(4));
        weatherIcon=label("◌",compact?24:32,0xffedb85c,false);weatherIcon.setGravity(Gravity.CENTER);weatherRow.addView(weatherIcon,lp(dp(compact?38:48),-1,0));
        weatherTemperature=label("-- °C",compact?25:32,WHITE,false);weatherRow.addView(weatherTemperature,lp(dp(compact?82:105),-1,0));
        LinearLayout weatherDetails=vertical();weatherDetails.setGravity(Gravity.CENTER_VERTICAL);
        weatherCondition=label("Pendiente de ubicación",compact?11:15,WHITE,true);weatherCondition.setMaxLines(1);weatherCondition.setEllipsize(TextUtils.TruncateAt.END);
        weatherDetails.addView(weatherCondition,lp(-1,dp(compact?20:26),0));weatherRow.addView(weatherDetails,lp(0,-1,1));
        weatherUpdated=label("Sin actualización",compact?9:11,SECONDARY,false);weatherUpdated.setMaxLines(2);weatherUpdated.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        weatherRow.addView(weatherUpdated,lp(dp(compact?74:95),-1,0));
        weatherRefreshButton=new ImageButton(this);weatherRefreshButton.setTag("weather_refresh");
        weatherRefreshButton.setImageResource(R.drawable.ic_weather_refresh);
        weatherRefreshButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        weatherRefreshButton.setPadding(dp(11),dp(11),dp(11),dp(11));
        bindPalette(p->{
            weatherRefreshButton.setImageTintList(new android.content.res.ColorStateList(
                    new int[][]{new int[]{-android.R.attr.state_enabled},new int[]{}},new int[]{p.secondary,p.text}));
            weatherRefreshButton.setBackground(buttonBackground(p,24));
        });
        weatherRefreshButton.setContentDescription("Actualizar clima con la ubicación GPS actual");
        weatherRefreshButton.setOnClickListener(v->refreshWeatherManually());
        LinearLayout.LayoutParams refreshParams=lp(dp(48),dp(48),0);refreshParams.leftMargin=dp(6);weatherRow.addView(weatherRefreshButton,refreshParams);
        weatherCard.addView(weatherRow,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams weatherParams=lp(-1,dp(compact?56:76),0);weatherParams.topMargin=gap;weatherParams.bottomMargin=gap;right.addView(weatherCard,weatherParams);

        mediaPanel=panel();mediaPanel.setTag("media_panel");mediaPanel.setPadding(dp(compact?10:16),dp(compact?8:12),dp(compact?10:16),dp(compact?8:12));
        LinearLayout mediaHeader=new LinearLayout(this);mediaHeader.setGravity(Gravity.CENTER_VERTICAL);
        mediaStatus=label("REPRODUCCIÓN",compact?9:11,SECONDARY,true);mediaStatus.setSingleLine();mediaStatus.setEllipsize(TextUtils.TruncateAt.END);
        mediaHeader.addView(mediaStatus,lp(0,-1,1));
        playbackLabel=caption("");playbackLabel.setLetterSpacing(.06f);bindPalette(p->playbackLabel.setTextColor(p.accent));mediaHeader.addView(playbackLabel);
        mediaPanel.addView(mediaHeader,lp(-1,dp(compact?18:22),0));
        ArtworkStage mediaBody=new ArtworkStage(this);mediaBody.setTag("artwork_stage");
        FrameLayout cover=new FrameLayout(this);artworkCover=cover;cover.setTag("media_artwork_cover");
        bindPalette(p->cover.setBackground(gradient(0xff183548,0xff0c1824,14)));cover.setClipToOutline(true);
        ImageView fallback=new ImageView(this);fallback.setImageDrawable(new DockIconDrawable("equalizer",0xff64c5d7));artworkFallback=fallback;
        cover.addView(artworkFallback,new FrameLayout.LayoutParams(dp(compact?46:80),dp(compact?46:80),Gravity.CENTER));
        albumArt=new ImageView(this);albumArt.setScaleType(ImageView.ScaleType.CENTER_CROP);albumArt.setContentDescription("Carátula publicada por la aplicación");
        cover.addView(albumArt,new FrameLayout.LayoutParams(-1,-1));
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x05000000,0xe6000000}));
        cover.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        cover.setOnClickListener(v->openMediaSource());cover.setOnLongClickListener(v->{chooseMediaSource();return true;});cover.setContentDescription("Elegir aplicación multimedia");
        LinearLayout details=vertical();details.setGravity(Gravity.CENTER_VERTICAL);
        details.setPadding(dp(12),dp(4),dp(12),dp(4));
        track=label("",compact?15:22,WHITE,true,false);track.setMaxLines(compact?1:2);track.setEllipsize(TextUtils.TruncateAt.END);
        details.addView(track,lp(-1,dp(compact?26:56),0));
        artistLabel=label("",compact?11:15,SECONDARY,false,false);artistLabel.setMaxLines(1);artistLabel.setEllipsize(TextUtils.TruncateAt.END);details.addView(artistLabel,lp(-1,dp(compact?18:24),0));
        cover.addView(details,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        mediaBody.addView(cover,new FrameLayout.LayoutParams(-1,-1));mediaPanel.addView(mediaBody,lp(-1,0,1));
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);
        previousButton=mediaButton(controls,"‹‹",-1,"Pista anterior");playButton=mediaButton(controls,"▶",0,"Reproducir o pausar");nextButton=mediaButton(controls,"››",1,"Pista siguiente");
        mediaPanel.addView(controls,lp(-1,dp(compact?48:52),0));setControls(null);right.addView(mediaPanel,lp(-1,0,1));
        center.addView(right,lp(0,-1,53));
        LinearLayout.LayoutParams centerParams=lp(-1,0,1);centerParams.topMargin=gap;centerParams.bottomMargin=gap;root.addView(center,centerParams);

        LinearLayout dock=new LinearLayout(this);dock.setTag("custom_dock");dock.setGravity(Gravity.CENTER_VERTICAL);
        addDockButton(dock,"Radio","radio",0xffffb957,"com.navimods.radio");
        addDockButton(dock,"Waze","navigation",0xff58aaff,"com.waze");
        addDockButton(dock,"Google Maps","maps",0xff5bcfff,"com.google.android.apps.maps");
        addDockButton(dock,"Amazon Music","equalizer",0xff36dfec,"com.amazon.mp3");
        addDockButton(dock,"YouTube Music","play",0xffff5666,"com.google.android.apps.youtube.music");
        root.addView(dock,lp(-1,dp(compact?88:120),0));
    }

    private Button actionButton(String text) {
        Button b=new Button(this);b.setText(text);b.setTextColor(WHITE);b.setTextSize(12);b.setAllCaps(false);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);
        bindPalette(p->{b.setTextColor(p.text);b.setBackground(buttonBackground(p,14));});return b;
    }
    private android.graphics.drawable.StateListDrawable buttonBackground(DashboardPalette palette,float radius) {
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},background(palette.pressed,radius));
        states.addState(new int[]{},background(palette.button,radius));return states;
    }
    private void addDockButton(LinearLayout dock,String name,String symbol,int color,String packageName) {
        LinearLayout tile=vertical();tile.setTag(packageName);tile.setGravity(Gravity.CENTER);
        bindPalette(p->{
            int glyph=p.dockColor(symbol,color);
            android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
            GradientDrawable pressed=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    p.light?new int[]{p.pressed,p.button}:new int[]{0xff252a30,0xff181c22});
            pressed.setCornerRadius(dp(18));pressed.setStroke(dp(1.5f),p.light?glyph:(glyph&0x00ffffff)|0x99000000);
            GradientDrawable focused=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    p.light?new int[]{p.button,p.panelBottom}:new int[]{0xff20252a,0xff15191e});
            focused.setCornerRadius(dp(18));focused.setStroke(dp(1),p.light?glyph:(glyph&0x00ffffff)|0x77000000);
            GradientDrawable normal=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    p.light?new int[]{p.panelTop,p.panelBottom}:new int[]{0xff191c20,0xff101215});
            normal.setCornerRadius(dp(18));normal.setStroke(dp(1.5f),p.border);
            states.addState(new int[]{android.R.attr.state_pressed},pressed);
            states.addState(new int[]{android.R.attr.state_focused},focused);states.addState(new int[]{},normal);
            tile.setBackground(new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf((glyph&0x00ffffff)|0x22000000),states,background(WHITE,18)));
        });
        tile.setClipToOutline(true);tile.setClickable(true);tile.setFocusable(true);tile.setContentDescription("Abrir "+name);
        ImageView icon=new ImageView(this);bindPalette(p->icon.setImageDrawable(new DockIconDrawable(symbol,p.dockColor(symbol,color),p.light)));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(icon,lp(dp(compact?48:64),dp(compact?48:64),0));
        TextView title=label(name,compact?11:13,WHITE,true);title.setGravity(Gravity.CENTER);
        title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams=lp(-1,dp(24),0);titleParams.topMargin=dp(compact?3:5);tile.addView(title,titleParams);
        LinearLayout.LayoutParams params=lp(0,-1,1);params.setMargins(dp(5),0,dp(5),0);dock.addView(tile,params);
        tile.setOnClickListener(v->{
            if(MediaSources.supported(packageName)&&mediaCoordinator!=null)mediaCoordinator.setPreferredPackage(null);
            sourceManager.launchApplication(name,packageName);
        });
    }
    private Button mediaButton(LinearLayout bar,String text,int action,String description) {
        Button b=actionButton(text);b.setTextSize(24);b.setContentDescription(description);
        LinearLayout.LayoutParams params=lp(dp(compact?70:88),dp(48),0);params.setMargins(dp(6),0,dp(6),0);bar.addView(b,params);
        b.setOnClickListener(v->{
            if(!MediaControlDispatcher.send(currentController,action)) {
                Toast.makeText(this,"Control no disponible para esta sesión",Toast.LENGTH_SHORT).show();
                refreshMedia();
            }
        });return b;
    }
    private void renderWeather(String result,boolean animate) {
        WeatherPresentation presentation=WeatherPresentation.parse(result);
        weatherTemperature.setText(presentation.temperature);
        weatherIcon.setText(presentation.icon);weatherScene.setWeather(presentation.icon);
        weatherCondition.setText(presentation.condition);
        weatherUpdated.setText("Actualizado "+DateFormat.getTimeFormat(this).format(new Date(weatherWallTime)));
        if(animate && animationsAllowed()) {
            weatherTemperature.setAlpha(.5f);weatherTemperature.animate().alpha(1).setDuration(300).start();
        }
    }
    private boolean animationsAllowed() {
        ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        return ValueAnimator.areAnimatorsEnabled() && am!=null && !am.isLowRamDevice() && pm!=null && !pm.isPowerSaveMode();
    }
    private void setControls(PlaybackState state) {
        long actions=state==null?0:state.getActions();
        boolean playing=state!=null && state.getState()==PlaybackState.STATE_PLAYING;
        boolean connecting=state!=null&&(state.getState()==PlaybackState.STATE_BUFFERING||state.getState()==PlaybackState.STATE_CONNECTING);
        boolean radio=MediaSources.NAVRADIO.equals(displayedMediaPackage);
        previousButton.setEnabled((actions&PlaybackState.ACTION_SKIP_TO_PREVIOUS)!=0);
        nextButton.setEnabled((actions&PlaybackState.ACTION_SKIP_TO_NEXT)!=0);
        long playAction=MediaControlDispatcher.playPauseAction(state);
        playButton.setEnabled(playAction!=0);
        boolean pauseAction=playAction==PlaybackState.ACTION_PAUSE||(playAction==PlaybackState.ACTION_PLAY_PAUSE&&(playing||connecting));
        playButton.setText(pauseAction?"Ⅱ":"▶");
        previousButton.setContentDescription(radio?"Emisora anterior":"Pista anterior");
        nextButton.setContentDescription(radio?"Emisora siguiente":"Pista siguiente");
        playButton.setContentDescription(radio?(pauseAction?"Pausar radio":"Reanudar radio"):(pauseAction?"Pausar":"Reproducir"));
        for(Button button:new Button[]{previousButton,playButton,nextButton}) {
            button.setVisibility(button.isEnabled()?View.VISIBLE:View.GONE);
            button.setAlpha(button.isEnabled()?1f:.28f);
        }
        playbackLabel.setText(state==null?"":playing?"●  EN REPRODUCCIÓN":connecting?"●  CONECTANDO":state.getState()==PlaybackState.STATE_PAUSED?"●  EN PAUSA":"●  DETENIDO");
        if(!playing || !resumed || !animationsAllowed()) {
            if(playbackPulse!=null){playbackPulse.cancel();playbackPulse=null;}playbackLabel.setAlpha(1);
        } else if(playbackPulse==null) {
            playbackPulse=ValueAnimator.ofFloat(.55f,1f);playbackPulse.setDuration(1800);playbackPulse.setRepeatMode(ValueAnimator.REVERSE);playbackPulse.setRepeatCount(ValueAnimator.INFINITE);
            playbackPulse.addUpdateListener(a->playbackLabel.setAlpha((float)a.getAnimatedValue()));playbackPulse.start();
        }
    }
    private void clearMedia(String message) {
        currentController=null;displayedMediaPackage=null;track.setText("");artistLabel.setText("");mediaStatus.setText("");setControls(null);
        mediaKey="";artworkLoader.clear();albumArt.setImageDrawable(null);artworkFallback.setVisibility(View.VISIBLE);
        ((ImageView)artworkFallback).setImageDrawable(new DockIconDrawable("equalizer",0xff64c5d7));
        artworkCover.setContentDescription("Elegir aplicación multimedia");
        mediaTint=null;updateMediaBackground();
    }
    // Retained for the existing UI fixture; runtime data comes from session callbacks.
    private void showMedia(MediaController selected) {
        renderMedia(selected,selected.getMetadata(),selected.getPlaybackState(),selected.getPackageName(),true);
    }
    private static String firstText(String... values) {
        for(String value:values)if(value!=null&&!value.trim().isEmpty())return value.trim();
        return "";
    }
    private void renderMedia(MediaController selected,MediaMetadata metadata,PlaybackState state,String pkg,boolean artworkChanged) {
        if(selected==null) {
            clearMedia("");displayedMediaPackage=MediaSources.supported(pkg)?pkg:null;
            if(displayedMediaPackage!=null) {
                mediaStatus.setText(MediaSources.name(pkg));
                if(MediaSources.NAVRADIO.equals(pkg))((ImageView)artworkFallback).setImageDrawable(new DockIconDrawable("radio",0xffffb957));
                artworkCover.setContentDescription("Abrir "+MediaSources.name(pkg)+"; mantener pulsado para elegir fuente");
            }
            return;
        }
        // A label, image and controls always belong to the same exact session.
        pkg=selected.getPackageName();
        boolean changed=currentController==null||!currentController.getSessionToken().equals(selected.getSessionToken());
        if(changed){artworkLoader.clear();albumArt.setImageDrawable(null);artworkFallback.setVisibility(View.VISIBLE);}
        currentController=selected;displayedMediaPackage=pkg;
        boolean radio=MediaSources.NAVRADIO.equals(pkg);
        albumArt.setScaleType(radio?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);
        ((ImageView)artworkFallback).setImageDrawable(new DockIconDrawable(radio?"radio":"equalizer",radio?0xffffb957:0xff64c5d7));
        String title=metadata==null?"":firstText(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
        String subtitle=metadata==null?"":firstText(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE),metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST));
        if(radio&&metadata!=null) {
            String extra=firstText(metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION),metadata.getString(MediaMetadata.METADATA_KEY_ALBUM));
            if(!extra.isEmpty()&&!extra.equals(title)&&!extra.equals(subtitle))subtitle=subtitle.isEmpty()?extra:subtitle+" · "+extra;
        }
        track.setText(title.isEmpty()?MediaSources.name(pkg):title);artistLabel.setText(subtitle);
        mediaStatus.setText(MediaSources.name(pkg));
        artworkCover.setContentDescription("Abrir "+MediaSources.name(pkg)+"; mantener pulsado para elegir fuente");
        setControls(state);
        if(artworkChanged||changed)artworkRevision++;
        String key=pkg+"|"+selected.getSessionToken()+"|"+artworkRevision;
        if(!key.equals(mediaKey)) {
            mediaKey=key;albumArt.setImageDrawable(null);artworkFallback.setVisibility(View.VISIBLE);
            final String requestedKey=key;
            artworkLoader.load(metadata,(bitmap,tint)->{
                if(destroyed||!requestedKey.equals(mediaKey))return;
                albumArt.setImageBitmap(bitmap);artworkFallback.setVisibility(bitmap==null?View.VISIBLE:View.INVISIBLE);
                mediaTint=tint;updateMediaBackground();
                // Artwork must remain visible across rapid metadata/state changes and pause/resume.
                albumArt.animate().cancel();albumArt.setImageAlpha(255);albumArt.setAlpha(1f);
            });
        }
    }
    private void openMediaSource() {
        if(MediaSources.supported(displayedMediaPackage))sourceManager.launchApplication(MediaSources.name(displayedMediaPackage),displayedMediaPackage);
        else chooseMediaSource();
    }
    private void chooseMediaSource() {
        String[] labels={"Automático","Amazon Music","YouTube Music","NavRadio+"};
        android.view.ContextThemeWrapper dialogTheme=new android.view.ContextThemeWrapper(this,displayPalette.light
                ?android.R.style.Theme_Material_Light_Dialog_Alert:android.R.style.Theme_Material_Dialog_Alert);
        new android.app.AlertDialog.Builder(dialogTheme).setTitle("Fuente del widget").setItems(labels,(dialog,which)->{
            if(which==0){mediaCoordinator.setPreferredPackage(null);return;}
            String pkg=MediaSources.PACKAGES[which-1];mediaCoordinator.setPreferredPackage(pkg);
            sourceManager.launchApplication(MediaSources.name(pkg),pkg);
        }).show();
    }
    private void refreshMedia() { if(mediaCoordinator!=null)mediaCoordinator.refresh(); }
    private void setGpsUnknown(String reason) {
        gpsUnavailableReason=reason;speedometer.setUnknown(reason.toUpperCase(Locale.ROOT));suvView.setGpsSpeed(Float.NaN);updateGpsTelemetry();
    }
    private void askLocationAndStart(){
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            lastGpsLocation=null;setGpsUnknown("Permiso GPS pendiente");
            if(!locationPermissionRequest){locationPermissionRequest=true;DashboardRecoveryController.get(this).userInteraction();
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},101);}return;
        }
        if(resumed)connectGps();
    }
    private void connectGps(){
        if(!GpsLocationService.hasPermission(this)){lastGpsLocation=null;setGpsUnknown("Permiso GPS pendiente");return;}
        GpsLocationService.startContinuousFromVisible(this);
        if(gpsBound)return;
        try {
            gpsBound=bindService(new Intent(this,GpsLocationService.class),gpsConnection,BIND_AUTO_CREATE);
            if(!gpsBound)setGpsUnknown("Servicio GPS no disponible");
        }catch(RuntimeException denied){gpsBound=false;setGpsUnknown("Servicio GPS no disponible");android.util.Log.w(GpsLocationService.TAG,"GPS binding failed",denied);}
    }
    // Also used by the separate instrumentation APK to isolate its explicit GPS fixtures.
    void disconnectGps(){
        if(gpsService!=null){gpsService.removeListener(gpsListener);gpsService=null;}
        if(gpsBound){gpsBound=false;try{unbindService(gpsConnection);}catch(IllegalArgumentException ignored){}}
    }
    private void refreshSpeedValidity() {
        updateGpsTelemetry();
        Location location=lastGpsLocation;
        if(location==null){setGpsUnknown(gpsUnavailableReason);return;}
        long now=android.os.SystemClock.elapsedRealtimeNanos();
        if(now<location.getElapsedRealtimeNanos() || now-location.getElapsedRealtimeNanos()>GpsSpeedState.MAX_AGE_NANOS) {
            setGpsUnknown("Sin señal GPS reciente");return;
        }
        if(!GpsSpeedState.usable(location.hasSpeed(),location.getSpeed(),location.getElapsedRealtimeNanos(),now)) {
            setGpsUnknown("Velocidad GPS no disponible");return;
        }
        float kmh=location.getSpeed()*3.6f;
        if(Float.compare(speedometer.getGpsSpeed(),kmh)!=0)speedometer.setGpsSpeed(kmh,resumed&&animationsAllowed());
        suvView.setGpsSpeed(kmh);
        updateGpsTelemetry();
    }
    public void setGpsAccuracyThresholds(float greenMeters,float yellowMeters) {
        GpsTelemetry.accuracyColor(false,false,0,greenMeters,yellowMeters);
        gpsGreenMeters=greenMeters;gpsYellowMeters=yellowMeters;
        getSharedPreferences("cockpit_gps",MODE_PRIVATE).edit().putFloat("green_m",greenMeters).putFloat("yellow_m",yellowMeters).apply();updateGpsTelemetry();
    }
    private void updateGpsTelemetry() {
        Location fix=lastGpsLocation;long now=android.os.SystemClock.elapsedRealtimeNanos();
        boolean fresh=fix!=null&&fix.getElapsedRealtimeNanos()>0&&now>=fix.getElapsedRealtimeNanos()&&now-fix.getElapsedRealtimeNanos()<=GpsSpeedState.MAX_AGE_NANOS;
        boolean accuracy=fresh&&fix.hasAccuracy()&&Float.isFinite(fix.getAccuracy())&&fix.getAccuracy()>=0;
        int color=GpsTelemetry.accuracyColor(fresh,accuracy,accuracy?fix.getAccuracy():0,gpsGreenMeters,gpsYellowMeters);
        gpsStatus.setText(accuracy?"GPS "+Math.round(fix.getAccuracy())+" m":"GPS -- m");
        gpsIndicator.setAccuracy(color,accuracy?"Precisión horizontal GPS "+Math.round(fix.getAccuracy())+" metros":"GPS sin precisión horizontal válida");
        String direction=fix==null?"--":GpsTelemetry.direction(fresh,fix.hasSpeed(),fix.getSpeed(),fix.hasBearing(),fix.getBearing(),fix.hasBearingAccuracy(),fix.getBearingAccuracyDegrees());
        compassStatus.setText(direction);compassIndicator.setBearing(direction.equals("--")?Float.NaN:fix.getBearing());
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==101){locationPermissionRequest=false;
            if(resumed){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)connectGps();
                DashboardRecoveryController.get(this).onMainResumed(this);}}
    }
    @Override public void onLocationChanged(Location location) {
        if(location==null || !LocationManager.GPS_PROVIDER.equals(location.getProvider()))return;
        lastGpsLocation=new Location(location);refreshSpeedValidity();
        long now=android.os.SystemClock.elapsedRealtimeNanos();
        if(location.getElapsedRealtimeNanos()>0 && now>=location.getElapsedRealtimeNanos()
                && now-location.getElapsedRealtimeNanos()<=GpsSpeedState.MAX_AGE_NANOS)updateWeather(location);
    }
    // Explicit on API 26-29, where LocationListener does not yet supply a default implementation.
    @Override public void onStatusChanged(String provider,int status,Bundle extras) {
        // Provider status is not a speed measurement. Freshness is evaluated from GPS fixes.
    }
    @Override public void onProviderEnabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider))askLocationAndStart();}
    @Override public void onProviderDisabled(String provider){if(LocationManager.GPS_PROVIDER.equals(provider)){lastGpsLocation=null;setGpsUnknown("GPS apagado");}}
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("weather_display",lastWeatherDisplay);state.putLong("weather_time",weatherWallTime);
        state.putLong("weather_success",lastWeatherSuccess);state.putLong("weather_attempt",weatherLoading?0:lastWeatherAttempt);
        state.putParcelable("weather_location",lastWeatherLocation);state.putParcelable("gps_fix",lastGpsLocation);
    }
    @Override protected void onResume(){
        super.onResume();suvView.applyAppearance(appearanceManager.load());displayController.start();
        applyImmersiveMode();resumed=true;mediaCoordinator.start();dateLabel.start();weatherScene.setRunning(true);speedometer.setRunning(true);suvView.setRunning(true,animationsAllowed());lightingManager.start();
        handler.removeCallbacks(ticker);handler.post(ticker);
        connectGps();
        if(!locationPermissionRequest)DashboardRecoveryController.get(this).onMainResumed(this);
    }
    @Override protected void onPause(){
        displayController.stop();DashboardRecoveryController.get(this).onMainPaused(this);disconnectGps();
        resumed=false;mediaCoordinator.stop();artworkLoader.clear();mediaKey="";dateLabel.stop();lightingManager.stop();weatherScene.setRunning(false);speedometer.setRunning(false);suvView.setRunning(false,false);
        if(playbackPulse!=null){playbackPulse.cancel();playbackPulse=null;}super.onPause();handler.removeCallbacks(ticker);
    }
    @Override public void onUserInteraction(){super.onUserInteraction();DashboardRecoveryController.get(this).userInteraction();}
    @Override protected void onDestroy(){destroyed=true;if(displayController!=null)displayController.close();disconnectGps();DashboardRecoveryController.get(this).onMainDestroyed(this);super.onDestroy();if(sourceManager!=null)sourceManager.close();if(mediaCoordinator!=null)mediaCoordinator.close();artworkLoader.close();dateLabel.close();lightingManager.close();suvView.close();handler.removeCallbacksAndMessages(null);}
}
