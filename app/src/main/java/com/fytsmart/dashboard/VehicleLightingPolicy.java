package com.fytsmart.dashboard;

import java.time.ZonedDateTime;

/** Pure time/source policy. No car I/O and no dependency on location or internet. */
public final class VehicleLightingPolicy {
    public enum Source { SCHEDULE_DECORATIVE, CANBUS_VERIFIED }
    public static final class State {
        public final boolean on;
        public final Source source;
        State(boolean on,Source source){this.on=on;this.source=source;}
    }
    public static boolean scheduledOn(int minute,int on,int off) {
        validate(on,off);
        if(minute<0||minute>=1440)throw new IllegalArgumentException("Invalid local minute");
        return on>off?minute>=on||minute<off:minute>=on&&minute<off;
    }
    static void validate(int on,int off) {
        if(on<0||on>=1440||off<0||off>=1440||on==off)throw new IllegalArgumentException("Distinct minutes 0..1439 required");
    }
    static State evaluate(ZonedDateTime now,int on,int off,boolean authorized,boolean verified,
                          Boolean signal,long ageMs,long ttlMs) {
        validate(on,off);
        if(authorized&&verified&&signal!=null&&ageMs>=0&&ageMs<ttlMs)
            return new State(signal,Source.CANBUS_VERIFIED);
        return new State(scheduledOn(now.getHour()*60+now.getMinute(),on,off),Source.SCHEDULE_DECORATIVE);
    }
    static ZonedDateTime nextBoundary(ZonedDateTime now,int on,int off) {
        validate(on,off);ZonedDateTime next=null;
        for(int day=0;day<3;day++)for(int minute:new int[]{on,off}) {
            ZonedDateTime candidate=now.toLocalDate().plusDays(day).atTime(minute/60,minute%60).atZone(now.getZone());
            if(candidate.isAfter(now)&&(next==null||candidate.isBefore(next)))next=candidate;
        }
        if(next==null)throw new IllegalStateException("No next boundary");
        return next;
    }
}
