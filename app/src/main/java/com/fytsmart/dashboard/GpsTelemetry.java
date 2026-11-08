package com.fytsmart.dashboard;

/** Independent of Android for reproducible tests. Horizontal accuracy is not satellite count. */
final class GpsTelemetry {
    static final int GREEN=0xff50dfa1,YELLOW=0xffffcc55,RED=0xffff6270;
    static int accuracyColor(boolean fresh,boolean hasAccuracy,float meters,float green,float yellow) {
        if(!(Float.isFinite(green)&&Float.isFinite(yellow)&&green>0&&yellow>green))throw new IllegalArgumentException("Invalid accuracy thresholds");
        if(!fresh||!hasAccuracy||Float.isNaN(meters)||Float.isInfinite(meters)||meters<0)return RED;
        return meters<=green?GREEN:meters<=yellow?YELLOW:RED;
    }
    static String direction(boolean fresh,boolean hasSpeed,float mps,boolean hasBearing,float bearing,
                            boolean hasBearingAccuracy,float bearingAccuracy) {
        if(!fresh||!hasSpeed||!Float.isFinite(mps)||mps<1.5f||!hasBearing||!Float.isFinite(bearing)||bearing<0||bearing>=360
                ||(hasBearingAccuracy&&(!Float.isFinite(bearingAccuracy)||bearingAccuracy<0||bearingAccuracy>30)))return "--";
        return new String[]{"N","NE","E","SE","S","SO","O","NO"}[(int)Math.floor((bearing+22.5f)/45f)%8];
    }
}
