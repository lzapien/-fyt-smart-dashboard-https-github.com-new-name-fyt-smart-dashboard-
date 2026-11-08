package com.fytsmart.dashboard;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Internal settings, reached by holding SOURCE. Normal SOURCE tap still opens the same drawer. */
public final class DashboardSettingsActivity extends Activity {
    private DashboardPreferences settings;
    private LinearLayout content;
    private Switch continuous;
    private ThemeManager themeManager;
    private DashboardPalette displayPalette=DashboardPalette.DARK;
    private DisplayModeController displayController;
    private DisplayPreferences displayPreferences;
    private ScrollView settingsRoot;
    private TextView displayStatus;
    private void applyDisplayPalette(DashboardPalette palette) {
        displayPalette=palette;settingsRoot.setBackgroundColor(palette.background);
        DisplayUi.apply(content,palette);
        appearancePreview.setPalette(palette);
        appearancePreview.setBackgroundColor(palette.light?palette.panelBottom:0xff02060b);
        updateDisplayStatus();
    }
    private void updateDisplayStatus() {
        displayStatus.setText(getString(R.string.display_status,displayController.current().light
                ?DisplayMode.LIGHT.label:DisplayMode.DARK.label,displayPreferences.load().label));
    }
    private void addDisplayMode() {
        TextView heading=text("Pantalla · Claro y oscuro",22);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        heading.setPadding(0,dp(20),0,dp(8));content.addView(heading);
        Spinner selector=new Spinner(this);selector.setTag("display_mode");
        selector.setContentDescription("Apariencia de pantalla: Automático, Claro u Oscuro");
        DisplayMode[] modes=DisplayMode.values();String[] labels=new String[modes.length];
        for(int i=0;i<modes.length;i++)labels[i]=modes[i].label;
        selector.setAdapter(DisplayUi.spinnerAdapter(this,labels,()->displayPalette));
        selector.setSelection(displayPreferences.load().ordinal());
        content.addView(selector,new LinearLayout.LayoutParams(-1,dp(52)));
        displayStatus=text("",16);displayStatus.setTag("display_status");content.addView(displayStatus);updateDisplayStatus();
        description("Automático: Claro de 07:00 a 17:59 y Oscuro de 18:00 a 06:59. Claro y Oscuro mantienen tu elección todo el día. Se guarda al seleccionar; no cambia el brillo del equipo ni los adornos del vehículo.");
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){
                DisplayMode mode=modes[position];
                if(displayPreferences.load()!=mode) { displayPreferences.save(mode);displayController.refresh(); }
                updateDisplayStatus();
            }
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
    }
    private EditText plateInput;
    private Spinner themeSelector;
    private Switch decorativeAnimations;
    private SuvRearView appearancePreview;
    private String previewPlate;
    private boolean appearanceResumed;
    private final Rect previewVisible=new Rect();
    private final Runnable previewRunning=this::updatePreviewRunning;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); settings = new DashboardPreferences(this);
        themeManager=new ThemeManager(this);
        displayPreferences=new DisplayPreferences(this);
        displayController=new DisplayModeController(this,this::applyDisplayPalette);
        displayPalette=displayController.current();
        DashboardRecoveryController.get(this).configurationChanged();
        ScrollView scroll = new ScrollView(this);settingsRoot=scroll;scroll.setBackgroundColor(displayPalette.background);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(16), dp(24), dp(24)); scroll.addView(content); setContentView(scroll);
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        Button back = new Button(this); back.setText("Volver"); back.setAllCaps(false); back.setOnClickListener(v -> finish());
        header.addView(back, new LinearLayout.LayoutParams(dp(120), dp(52)));
        TextView title = text("Ajustes del dashboard", 24); title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setPadding(dp(20), 0, 0, 0);
        header.addView(title); content.addView(header);
        addDisplayMode();
        addPersonalization(state);
        TextView operationTitle=text("Arranque y GPS",22);operationTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);content.addView(operationTitle);
        description("Google Maps y NavRadio+ conservan su inicio automático del sistema. El dashboard no vuelve a abrirlas ni cambia su reproducción.");
        option("Mostrar dashboard al iniciar Android", DashboardPreferences.RECOVER_BOOT, settings.recoverBoot());
        option("Mostrar dashboard al volver a encender la pantalla", DashboardPreferences.RECOVER_WAKE, settings.recoverWake());
        description("Se solicita volver al dashboard una sola vez. Si comienzas a usarlo durante la espera, se cancela la recuperación pendiente.");
        choices("Espera antes de mostrar el dashboard", DashboardPreferences.RECOVERY_DELAY,
                new String[]{"3 segundos", "5 segundos", "8 segundos", "12 segundos", "15 segundos"},
                new long[]{3_000, 5_000, 8_000, 12_000, 15_000}, settings.recoveryDelayMs());
        continuous = option("GPS continuo con notificación", DashboardPreferences.GPS_CONTINUOUS, settings.continuousGps());
        description("Solicita ubicación también mientras utilizas otra aplicación. Puedes detenerlo aquí o desde su notificación. Al desactivarlo, el dashboard conserva GPS mientras está visible.");
        choices("Intervalo GPS solicitado en movimiento", DashboardPreferences.GPS_MOVING,
                new String[]{"2 segundos", "3 segundos", "5 segundos"}, new long[]{2_000, 3_000, 5_000}, settings.movingMs());
        choices("Intervalo GPS solicitado al estar detenido", DashboardPreferences.GPS_IDLE,
                new String[]{"15 segundos", "20 segundos", "30 segundos"}, new long[]{15_000, 20_000, 30_000}, settings.idleMs());
        description("La recepción real depende del GPS y de Android. Este servicio no activa navegación ni controla la ubicación interna de Google Maps.");
        scroll.setOnScrollChangeListener((view,x,y,oldX,oldY)->updatePreviewRunning());
    }
    private void addPersonalization(Bundle state) {
        ThemeManager.Selection saved=themeManager.load();previewPlate=saved.plate;
        TextView title=text("Personalización",22);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setPadding(0,dp(16),0,dp(8));content.addView(title);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);
        form.addView(text("Matrícula",16));
        plateInput=new EditText(this);plateInput.setTag("appearance_plate");plateInput.setSingleLine(true);
        plateInput.setTextColor(0xfff6f8fc);plateInput.setHintTextColor(0xffa9bbd0);plateInput.setTextSize(18);
        plateInput.setHint(VehiclePlateConfig.DEFAULT);plateInput.setContentDescription("Matrícula mostrada en la camioneta");
        plateInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        plateInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(VehiclePlateConfig.MAX_LENGTH)});
        plateInput.setText(state==null?saved.plate:state.getString("appearance_plate",saved.plate));
        form.addView(plateInput,new LinearLayout.LayoutParams(-1,dp(48)));
        form.addView(text("Adornos del vehículo",16));
        themeSelector=new Spinner(this);themeSelector.setTag("appearance_theme");themeSelector.setContentDescription("Tema visual del vehículo");
        ThemeConfig[] themes=ThemeConfig.values();String[] labels=new String[themes.length];
        for(int i=0;i<themes.length;i++)labels[i]=themes[i].label;
        ArrayAdapter<String> adapter=DisplayUi.spinnerAdapter(this,labels,()->displayPalette);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);themeSelector.setAdapter(adapter);
        ThemeConfig selected=state==null?saved.theme:ThemeConfig.fromId(state.getString("appearance_theme",saved.theme.id));
        themeSelector.setSelection(selected.ordinal());form.addView(themeSelector,new LinearLayout.LayoutParams(-1,dp(48)));
        decorativeAnimations=new Switch(this);decorativeAnimations.setTag("appearance_animations");decorativeAnimations.setText(R.string.appearance_decorative_animations);
        decorativeAnimations.setTextColor(0xfff6f8fc);decorativeAnimations.setTextSize(16);decorativeAnimations.setMinHeight(dp(48));
        decorativeAnimations.setChecked(state==null?saved.animations:state.getBoolean("appearance_animations",saved.animations));form.addView(decorativeAnimations);
        Button save=new Button(this);save.setText(R.string.appearance_save);save.setAllCaps(false);save.setTag("appearance_save");
        save.setOnClickListener(v->{
            try {
                ThemeManager.Selection selection=themeManager.save(plateInput.getText().toString(),ThemeConfig.values()[themeSelector.getSelectedItemPosition()],decorativeAnimations.isChecked());
                previewPlate=selection.plate;plateInput.setText(selection.plate);plateInput.setError(null);updateAppearancePreview();
                Toast.makeText(this,"Personalización guardada",Toast.LENGTH_SHORT).show();
            }catch(IllegalArgumentException invalid){plateInput.setError(invalid.getMessage());plateInput.requestFocus();}
        });
        form.addView(save,new LinearLayout.LayoutParams(-1,dp(48)));row.addView(form,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout previewColumn=new LinearLayout(this);previewColumn.setOrientation(LinearLayout.VERTICAL);
        TextView previewLabel=text("Vista previa · sin guardar",14);previewLabel.setTextColor(0xffa9bbd0);previewLabel.setGravity(Gravity.CENTER);previewColumn.addView(previewLabel);
        appearancePreview=new SuvRearView(this);appearancePreview.setTag("appearance_preview");appearancePreview.setBackgroundColor(0xff02060b);
        previewColumn.addView(appearancePreview,new LinearLayout.LayoutParams(-1,dp(240)));
        LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(0,-2,1);previewParams.leftMargin=dp(16);row.addView(previewColumn,previewParams);content.addView(row);
        description("La vista previa no cambia el dashboard hasta Guardar. Al desactivar animaciones, los adornos permanecen estáticos. Original recupera el diseño actual.");
        plateInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){updateAppearancePreview();}
            @Override public void afterTextChanged(Editable text){}
        });
        themeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){updateAppearancePreview();}
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        decorativeAnimations.setOnCheckedChangeListener((button,checked)->updateAppearancePreview());
        appearancePreview.addOnLayoutChangeListener((view,l,t,r,b,ol,ot,or,ob)->updatePreviewRunning());
        updateAppearancePreview();
    }
    private void updateAppearancePreview() {
        if(appearancePreview==null)return;
        try{previewPlate=VehiclePlateConfig.normalize(plateInput.getText().toString());}catch(IllegalArgumentException ignored){}
        ThemeConfig theme=ThemeConfig.values()[Math.max(0,themeSelector.getSelectedItemPosition())];
        appearancePreview.applyAppearance(new ThemeManager.Selection(previewPlate,theme,decorativeAnimations.isChecked()));
        appearancePreview.setContentDescription("Vista previa "+theme.label+", matrícula "+previewPlate);
        updatePreviewRunning();
    }
    private void updatePreviewRunning() {
        if(appearancePreview==null)return;
        android.app.ActivityManager memory=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
        android.os.PowerManager power=(android.os.PowerManager)getSystemService(POWER_SERVICE);
        boolean allowed=android.animation.ValueAnimator.areAnimatorsEnabled()&&memory!=null&&!memory.isLowRamDevice()&&power!=null&&!power.isPowerSaveMode();
        appearancePreview.setRunning(appearanceResumed&&appearancePreview.getGlobalVisibleRect(previewVisible),allowed);
    }
    @Override protected void onResume(){super.onResume();displayController.start();appearanceResumed=true;appearancePreview.post(previewRunning);}
    @Override protected void onPause(){displayController.stop();appearanceResumed=false;appearancePreview.removeCallbacks(previewRunning);appearancePreview.setRunning(false,false);super.onPause();}
    @Override protected void onDestroy(){if(displayController!=null)displayController.close();appearanceResumed=false;if(appearancePreview!=null){appearancePreview.removeCallbacks(previewRunning);appearancePreview.close();}super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){
        state.putString("appearance_plate",plateInput.getText().toString());
        state.putString("appearance_theme",ThemeConfig.values()[Math.max(0,themeSelector.getSelectedItemPosition())].id);
        state.putBoolean("appearance_animations",decorativeAnimations.isChecked());super.onSaveInstanceState(state);
    }
    private Switch option(String label, String key, boolean checked) {
        Switch view = new Switch(this); view.setText(label); view.setTextSize(18); view.setTextColor(0xfff6f8fc);
        view.setPadding(0, dp(10), 0, dp(10)); view.setMinHeight(dp(56)); view.setChecked(checked);
        view.setOnCheckedChangeListener((button, enabled) -> {
            settings.values.edit().putBoolean(key, enabled).apply();
            DashboardRecoveryController.get(this).configurationChanged();
            if (DashboardPreferences.GPS_CONTINUOUS.equals(key)) {
                if (!enabled) GpsLocationService.stopContinuousFromVisible(this);
                else if (GpsLocationService.hasPermission(this)) GpsLocationService.startContinuousFromVisible(this);
                else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 102);
            }
        });
        content.addView(view, new LinearLayout.LayoutParams(-1, -2)); return view;
    }
    private void choices(String label, String key, String[] labels, long[] values, long selected) {
        content.addView(text(label, 16));
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = DisplayUi.spinnerAdapter(this,labels,()->displayPalette);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter);
        int position = 0;
        for (int i = 0; i < values.length; i++) if (values[i] == selected) position = i;
        spinner.setSelection(position);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                if (settings.values.getLong(key, selected) != values[index]) {
                    settings.values.edit().putLong(key, values[index]).apply();
                    DashboardRecoveryController.get(DashboardSettingsActivity.this).configurationChanged();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        content.addView(spinner, new LinearLayout.LayoutParams(-1, dp(52)));
    }
    private TextView text(String value, int size) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(0xfff6f8fc); return text;
    }
    private void description(String value) {
        TextView text = text(value, 14); text.setTextColor(0xffa9bbd0); text.setPadding(0, dp(12), 0, dp(16)); content.addView(text);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 102) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) GpsLocationService.startContinuousFromVisible(this);
            else continuous.setChecked(false);
        }
    }
}
