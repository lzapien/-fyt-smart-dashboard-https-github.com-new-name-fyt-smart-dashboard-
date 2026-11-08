package com.fytsmart.dashboard;
import java.time.*;
public final class VehicleLightingPolicyTest {
    static int count;
    static void check(boolean value,String label){if(!value)throw new AssertionError(label);count++;System.out.println("PASS,"+label);}
    static ZonedDateTime at(String time,String zone){return ZonedDateTime.of(LocalDate.of(2026,10,29),LocalTime.parse(time),ZoneId.of(zone));}
    public static void main(String[] args){
        String[] hours={"00:00","05:59","06:00","17:59","18:00","23:59"};boolean[] expected={true,true,false,false,true,true};
        for(String zone:new String[]{"America/Mexico_City","Asia/Tokyo","Europe/Madrid"})for(int i=0;i<hours.length;i++){
            VehicleLightingPolicy.State s=VehicleLightingPolicy.evaluate(at(hours[i],zone),1080,360,false,false,null,-1,30000);
            check(s.on==expected[i]&&s.source==VehicleLightingPolicy.Source.SCHEDULE_DECORATIVE,zone+" "+hours[i]+" -> "+s.on+" DECORATIVE");
        }
        check(VehicleLightingPolicy.nextBoundary(at("17:59","America/Mexico_City"),1080,360).getHour()==18,"next ON 18:00");
        ZonedDateTime off=VehicleLightingPolicy.nextBoundary(at("18:00","America/Mexico_City"),1080,360);
        check(off.getHour()==6&&off.getDayOfMonth()==30,"next OFF tomorrow 06:00");
        check(!VehicleLightingPolicy.evaluate(at("12:00","UTC"),1080,360,false,true,true,0,30000).on,"unauthorized CAN ignored");
        check(!VehicleLightingPolicy.evaluate(at("12:00","UTC"),1080,360,true,false,true,0,30000).on,"unverified CAN ignored");
        check(VehicleLightingPolicy.evaluate(at("12:00","UTC"),1080,360,true,true,true,0,30000).on,"verified CAN overrides daytime");
        check(!VehicleLightingPolicy.evaluate(at("20:00","UTC"),1080,360,true,true,false,0,30000).on,"verified CAN OFF overrides nighttime");
        check(VehicleLightingPolicy.evaluate(at("20:00","UTC"),1080,360,true,true,false,30000,30000).on,"expired CAN falls back to schedule");
        check(VehicleLightingPolicy.evaluate(at("20:00","UTC"),1080,360,true,true,null,-1,30000).on,"missing CAN falls back to schedule");
        check(VehicleLightingPolicy.scheduledOn(9*60,8*60,20*60),"custom daytime interval ON");
        check(!VehicleLightingPolicy.scheduledOn(21*60,8*60,20*60),"custom daytime interval OFF");
        boolean invalid=false;try{VehicleLightingPolicy.scheduledOn(0,60,60);}catch(IllegalArgumentException e){invalid=true;}check(invalid,"equal boundaries rejected");
        Instant same=Instant.parse("2026-01-01T01:00:00Z");
        check(VehicleLightingPolicy.scheduledOn(same.atZone(ZoneId.of("America/Mexico_City")).getHour()*60,1080,360)
                &&!VehicleLightingPolicy.scheduledOn(same.atZone(ZoneId.of("Asia/Tokyo")).getHour()*60,1080,360),"same instant uses local time zone");
        System.out.println("VehicleLightingPolicy: "+count+" checks passed; Android clocks were not changed.");
    }
}
