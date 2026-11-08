package com.fytsmart.dashboard;

import android.content.Context;
import android.content.SharedPreferences;

/** A separate private file deliberately leaves vehicle_appearance and GPS settings untouched. */
final class DisplayPreferences {
    static final String FILE="display_appearance", MODE="display.mode";
    private final SharedPreferences preferences;
    DisplayPreferences(Context context) { this(context.getSharedPreferences(FILE,Context.MODE_PRIVATE)); }
    DisplayPreferences(SharedPreferences preferences) { this.preferences=preferences; }
    DisplayMode load() {
        try { return DisplayMode.fromId(preferences.getString(MODE,DisplayMode.AUTO.id)); }
        catch(ClassCastException invalid) { return DisplayMode.AUTO; }
    }
    void save(DisplayMode mode) { preferences.edit().putString(MODE,mode.id).apply(); }
}
