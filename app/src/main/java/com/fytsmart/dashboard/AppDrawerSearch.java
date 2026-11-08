package com.fytsmart.dashboard;

import java.text.Normalizer;
import java.util.Locale;

/** Offline matching shared by the drawer and host-side checks. */
final class AppDrawerSearch {
    private AppDrawerSearch() {}

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    static boolean matches(String label, String packageName, String query) {
        String wanted = normalize(query);
        if (wanted.isEmpty()) return true;
        String haystack = normalize(label) + " " + normalize(packageName);
        for (String word : wanted.split("\\s+")) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }
}
