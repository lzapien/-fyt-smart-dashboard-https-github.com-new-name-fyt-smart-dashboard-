package com.fytsmart.dashboard;

/** Explicit supported applications, never a package-name heuristic or an OEM radio API. */
final class MediaSources {
    static final String AMAZON = "com.amazon.mp3";
    static final String YOUTUBE = "com.google.android.apps.youtube.music";
    // Confirmed by the prior FYT package dump and the developer's Google Play listing.
    static final String NAVRADIO = "com.navimods.radio";
    static final String[] PACKAGES = {AMAZON, YOUTUBE, NAVRADIO};
    private MediaSources() {}
    static boolean supported(String pkg) {
        return AMAZON.equals(pkg) || YOUTUBE.equals(pkg) || NAVRADIO.equals(pkg);
    }
    static String name(String pkg) {
        if (AMAZON.equals(pkg)) return "Amazon Music";
        if (YOUTUBE.equals(pkg)) return "YouTube Music";
        if (NAVRADIO.equals(pkg)) return "NavRadio+";
        return "";
    }
}
