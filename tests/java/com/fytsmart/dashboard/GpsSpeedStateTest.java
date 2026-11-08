package com.fytsmart.dashboard;
public final class GpsSpeedStateTest {
    private static int checks;
    static void check(boolean expected,boolean actual){if(expected!=actual)throw new AssertionError("GPS validity "+checks);checks++;}
    public static void main(String[] args){
        long now=100_000_000_000L;
        check(true,GpsSpeedState.usable(true,0,now,now));
        check(true,GpsSpeedState.usable(true,27,now-1_000_000_000L,now));
        check(false,GpsSpeedState.usable(false,0,now,now));
        check(false,GpsSpeedState.usable(true,Float.NaN,now,now));
        check(false,GpsSpeedState.usable(true,Float.POSITIVE_INFINITY,now,now));
        check(false,GpsSpeedState.usable(true,Float.MAX_VALUE,now,now));
        check(false,GpsSpeedState.usable(true,-1,now,now));
        check(false,GpsSpeedState.usable(true,10,0,now));
        check(false,GpsSpeedState.usable(true,10,now+1,now));
        check(false,GpsSpeedState.usable(true,10,now-16_000_000_000L,now));
        check(true,GpsSpeedState.usable(true,10,now-GpsSpeedState.MAX_AGE_NANOS,now));
        check(false,GpsSpeedState.usable(true,10,now-GpsSpeedState.MAX_AGE_NANOS-1,now));
        System.out.println("GpsSpeedState: "+checks+" checks passed");
    }
}
