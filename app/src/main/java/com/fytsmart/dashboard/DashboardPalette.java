package com.fytsmart.dashboard;

/** Opaque daylight surfaces and strong text/outline colors; the night palette keeps the cockpit. */
final class DashboardPalette {
    static final DashboardPalette LIGHT=new DashboardPalette(true), DARK=new DashboardPalette(false);
    final boolean light;
    final int background, panelTop, panelBottom, border, text, secondary, accent, button, pressed;
    private DashboardPalette(boolean light) {
        this.light=light;
        background=light?0xffdce3e9:0xff000000;
        panelTop=light?0xffffffff:0xff0d151f;
        panelBottom=light?0xffedf2f7:0xff070c12;
        border=light?0xff65778a:0xff89939d;
        text=light?0xff101e2d:0xfff6f8fc;
        secondary=light?0xff364c61:0xffa9bbd0;
        accent=light?0xff005a91:0xff37b8db;
        button=light?0xffd2e2ee:0xff213547;
        pressed=light?0xffb4cfdf:0xff32536a;
    }
    static DashboardPalette forLight(boolean light) { return light?LIGHT:DARK; }
    int textColor(int nightColor) {
        if(!light)return nightColor;
        if(nightColor==0xfff6f8fc) return text;
        if(nightColor==0xffa9bbd0) return secondary;
        if(nightColor==0xff37b8db) return accent;
        if(nightColor==0xffedb85c) return 0xff865000;
        return nightColor;
    }
    int dockColor(String symbol,int original) {
        if(!light)return original;
        switch(symbol) {
            case "radio": return 0xff865000;
            case "navigation": return 0xff005caa;
            case "equalizer": return 0xff006574;
            case "play": return 0xffb71930;
            default: return accent;
        }
    }
}
