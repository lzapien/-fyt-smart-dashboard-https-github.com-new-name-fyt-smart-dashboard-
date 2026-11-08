package com.fytsmart.dashboard;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DisplayModeTest {
    private static int passed;
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message);passed++; }
    private static ZonedDateTime local(String value) { return ZonedDateTime.parse(value+"-06:00[America/Mexico_City]"); }
    private static double luminance(int color) {
        double value=0;
        for(int i=0;i<3;i++) {
            double channel=((color>>(16-i*8))&255)/255d;
            channel=channel<=.04045?channel/12.92:Math.pow((channel+.055)/1.055,2.4);
            value+=channel*(i==0?.2126:i==1?.7152:.0722);
        }
        return value;
    }
    private static double contrast(int first,int second) {
        double a=luminance(first),b=luminance(second);
        return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    }
    public static void main(String[] args) {
        check(!DisplayMode.AUTO.isLight(local("2026-11-05T06:59:59.999999999")),"Still dark before 07:00");
        check(DisplayMode.AUTO.isLight(local("2026-11-05T07:00:00")),"Light starts exactly at 07:00");
        check(DisplayMode.AUTO.isLight(local("2026-11-05T17:59:59.999999999")),"Light includes the last moment before 18:00");
        check(!DisplayMode.AUTO.isLight(local("2026-11-05T18:00:00")),"Dark starts exactly at 18:00");
        check(!DisplayMode.AUTO.isLight(local("2026-11-05T00:00:00")),"Midnight remains dark");
        for(String id:new String[]{null,"","broken","LIGHT","christmas"})check(DisplayMode.fromId(id)==DisplayMode.AUTO,"Unknown value migrates safely to Auto");
        for(DisplayMode mode:DisplayMode.values())check(DisplayMode.fromId(mode.id)==mode,"Persisted selection survives reload");
        ZonedDateTime start=local("2026-11-05T00:00:00");
        for(int minute=0;minute<24*60;minute++) {
            ZonedDateTime now=start.plusMinutes(minute);
            check(DisplayMode.LIGHT.isLight(now),"Manual light ignores clock");
            check(!DisplayMode.DARK.isLight(now),"Manual dark ignores clock");
            check(DisplayMode.AUTO.isLight(now)==(minute>=420&&minute<1080),"Complete daily schedule");
        }
        check(DisplayMode.nextBoundary(local("2026-11-05T06:59:59")).equals(local("2026-11-05T07:00:00")),"Morning boundary");
        check(DisplayMode.nextBoundary(local("2026-11-05T07:00:00")).equals(local("2026-11-05T18:00:00")),"Morning does not reschedule itself");
        check(DisplayMode.nextBoundary(local("2026-11-05T18:00:00")).equals(local("2026-11-06T07:00:00")),"Evening schedules next morning");
        ZonedDateTime instant=local("2026-11-05T12:00:00");
        check(DisplayMode.AUTO.isLight(instant)&&!DisplayMode.AUTO.isLight(instant.withZoneSameInstant(ZoneId.of("Asia/Tokyo"))),"Same instant follows each local timezone");
        // Crossing midnight must honor the next date's zone offset, never assume a fixed 13-hour night.
        for(String evening:new String[]{"2026-03-07T18:00:00-05:00[America/New_York]","2026-10-31T18:00:00-04:00[America/New_York]"}) {
            ZonedDateTime now=ZonedDateTime.parse(evening),next=DisplayMode.nextBoundary(now);
            check(next.getHour()==7&&next.toLocalDate().equals(now.toLocalDate().plusDays(1)),"DST still changes at local 07:00");
            check(Duration.between(now,next).toHours()==(now.getMonthValue()==3?12:14),"DST delay uses actual elapsed time");
        }
        DashboardPalette daylight=DashboardPalette.LIGHT;
        for(int text:new int[]{daylight.text,daylight.secondary,daylight.accent})
            for(int surface:new int[]{daylight.background,daylight.panelTop,daylight.panelBottom,daylight.button,daylight.pressed})
                check(contrast(text,surface)>=4.5,"Day text contrast >= 4.5:1, text="+Integer.toHexString(text)+", surface="+Integer.toHexString(surface));
        // Do not silently change legacy night colors merely to meet a new daylight criterion.
        for(int text:new int[]{DashboardPalette.DARK.text,DashboardPalette.DARK.secondary})
            for(int surface:new int[]{DashboardPalette.DARK.background,DashboardPalette.DARK.panelTop,DashboardPalette.DARK.panelBottom})
                check(contrast(text,surface)>=4.5,"Existing night panel text remains readable");
        DashboardPalette day=DashboardPalette.LIGHT;
        for(String symbol:new String[]{"radio","navigation","equalizer","play"})
            check(contrast(day.dockColor(symbol,0),day.panelBottom)>=4.5,"Day dock symbol remains visible: "+symbol);
        check(DashboardPalette.DARK.background==0xff000000&&DashboardPalette.DARK.text==0xfff6f8fc,"Existing night appearance preserved");
        System.out.println("DisplayMode: "+passed+" checks passed (schedule, manual override, timezone/DST, contrast)");
    }
}
