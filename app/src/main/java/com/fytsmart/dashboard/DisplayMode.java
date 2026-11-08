package com.fytsmart.dashboard;

import java.time.LocalTime;
import java.time.ZonedDateTime;

/** Screen colors only: independent of seasonal decorations and virtual vehicle lighting. */
enum DisplayMode {
    AUTO("auto", "Automático"), LIGHT("light", "Claro"), DARK("dark", "Oscuro");
    final String id, label;
    DisplayMode(String id, String label) { this.id=id; this.label=label; }
    static DisplayMode fromId(String id) {
        for(DisplayMode mode:values()) if(mode.id.equals(id)) return mode;
        return AUTO;
    }
    boolean isLight(ZonedDateTime now) {
        return this==LIGHT || (this==AUTO && !now.toLocalTime().isBefore(LocalTime.of(7,0))
                && now.toLocalTime().isBefore(LocalTime.of(18,0)));
    }
    /** Strictly future local boundary; zone rules handle date/UTC-offset changes. */
    static ZonedDateTime nextBoundary(ZonedDateTime now) {
        ZonedDateTime morning=now.toLocalDate().atTime(7,0).atZone(now.getZone());
        ZonedDateTime evening=now.toLocalDate().atTime(18,0).atZone(now.getZone());
        if(now.isBefore(morning)) return morning;
        if(now.isBefore(evening)) return evening;
        return now.toLocalDate().plusDays(1).atTime(7,0).atZone(now.getZone());
    }
}
