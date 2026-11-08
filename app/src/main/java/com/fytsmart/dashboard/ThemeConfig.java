package com.fytsmart.dashboard;

/** Manual themes. The original theme has no decoration or animation. */
enum ThemeConfig {
    DEFAULT("default", "Original"), CHRISTMAS("christmas", "Navidad"), HALLOWEEN("halloween", "Halloween");
    final String id, label;
    ThemeConfig(String id, String label) { this.id = id; this.label = label; }
    static ThemeConfig fromId(String id) {
        for (ThemeConfig value : values()) if (value.id.equals(id)) return value;
        return DEFAULT;
    }
}
