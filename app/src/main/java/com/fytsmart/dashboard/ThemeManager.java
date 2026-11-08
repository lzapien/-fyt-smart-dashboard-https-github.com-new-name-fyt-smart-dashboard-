package com.fytsmart.dashboard;

import android.content.Context;
import android.content.SharedPreferences;

/** Explicit appearance saves, independent of the existing operational/GPS preferences. */
final class ThemeManager {
    static final String FILE = "vehicle_appearance";
    static final String PLATE = "vehicle.plate", THEME = "dashboard.theme", ANIMATIONS = "dashboard.animations";
    static final Selection DEFAULT = new Selection(VehiclePlateConfig.DEFAULT, ThemeConfig.DEFAULT, true);
    static final class Selection {
        final String plate;
        final ThemeConfig theme;
        final boolean animations;
        Selection(String plate, ThemeConfig theme, boolean animations) {
            this.plate = VehiclePlateConfig.normalize(plate);
            this.theme = theme == null ? ThemeConfig.DEFAULT : theme;
            this.animations = animations;
        }
    }
    interface Storage {
        String plate();
        String theme();
        boolean animations();
        void save(Selection selection);
    }
    static final class PreferencesStorage implements Storage {
        private final SharedPreferences preferences;
        PreferencesStorage(SharedPreferences preferences) { this.preferences = preferences; }
        @Override public String plate() {
            try { return preferences.getString(PLATE, VehiclePlateConfig.DEFAULT); }
            catch (ClassCastException invalid) { return VehiclePlateConfig.DEFAULT; }
        }
        @Override public String theme() {
            try { return preferences.getString(THEME, ThemeConfig.DEFAULT.id); }
            catch (ClassCastException invalid) { return ThemeConfig.DEFAULT.id; }
        }
        @Override public boolean animations() {
            try { return preferences.getBoolean(ANIMATIONS, true); }
            catch (ClassCastException invalid) { return true; }
        }
        @Override public void save(Selection selection) {
            // One atomic editor; disk I/O is handled asynchronously by SharedPreferences.
            preferences.edit().putString(PLATE, selection.plate).putString(THEME, selection.theme.id)
                    .putBoolean(ANIMATIONS, selection.animations).apply();
        }
    }
    private final Storage storage;
    ThemeManager(Context context) { this(new PreferencesStorage(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))); }
    ThemeManager(Storage storage) { this.storage = storage; }
    Selection load() { return new Selection(VehiclePlateConfig.fromStored(storage.plate()), ThemeConfig.fromId(storage.theme()), storage.animations()); }
    Selection save(String plate, ThemeConfig theme, boolean animations) {
        Selection selection = new Selection(plate, theme, animations);
        storage.save(selection); return selection;
    }
}
