package com.fytsmart.dashboard;

import java.util.Locale;

/** Display text only. Never reads or writes vehicle identifiers or CANBUS. */
final class VehiclePlateConfig {
    static final String DEFAULT = "TU-PLACA";
    static final int MAX_LENGTH = 12;
    private VehiclePlateConfig() { }
    static String normalize(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (text.isEmpty()) return DEFAULT;
        if (text.codePointCount(0, text.length()) > MAX_LENGTH)
            throw new IllegalArgumentException("Usa hasta 12 letras, números, espacios o guiones");
        boolean alphanumeric = false;
        for (int offset = 0; offset < text.length();) {
            int c = text.codePointAt(offset); offset += Character.charCount(c);
            if (Character.isLetterOrDigit(c)) alphanumeric = true;
            else if (c != ' ' && c != '-') throw new IllegalArgumentException("Usa letras, números, espacios o guiones");
        }
        if (!alphanumeric) throw new IllegalArgumentException("La matrícula debe incluir letras o números");
        return text;
    }
    static String fromStored(String value) {
        try { return normalize(value); }
        catch (IllegalArgumentException invalid) { return DEFAULT; }
    }
}
