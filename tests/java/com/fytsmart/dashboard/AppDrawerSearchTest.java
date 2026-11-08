package com.fytsmart.dashboard;

public final class AppDrawerSearchTest {
    private static int passed;
    private static void check(boolean valid) {
        if (!valid) throw new AssertionError("Search check " + (passed + 1));
        passed++;
    }
    public static void main(String[] args) {
        check(AppDrawerSearch.matches("Configuración", "com.android.settings", "configuracion"));
        check(AppDrawerSearch.matches("Maps", "com.google.android.apps.maps", "MAPS"));
        check(AppDrawerSearch.matches("Radio", "com.navimods.radio", "navimods"));
        check(AppDrawerSearch.matches("YouTube Music", "com.google.android.apps.youtube.music", "music youtube"));
        check(!AppDrawerSearch.matches("Maps", "com.google.android.apps.maps", "music"));
        check(AppDrawerSearch.matches("Radio", "com.navimods.radio", "  "));
        check(AppDrawerSearch.matches("Radio", "com.navimods.radio", null));
        check(!AppDrawerSearch.matches("Radio", "com.navimods.radio", "radio amazon"));
        check(AppDrawerSearch.matches("Reloj", "com.android.deskclock", "android reloj"));
        check(AppDrawerSearch.normalize("  ÁÉÍÓÚ Ñ  ").equals("aeiou n"));
        check(AppDrawerSearch.matches("Files", "com.google.android.documentsui", "files\tgoogle"));
        System.out.println("AppDrawerSearch: " + passed + " checks passed");
    }
}
