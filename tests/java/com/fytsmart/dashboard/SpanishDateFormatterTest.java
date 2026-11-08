package com.fytsmart.dashboard;
import java.time.*;
import java.util.Locale;
public final class SpanishDateFormatterTest {
    public static void main(String[] args){
        Locale previous=Locale.getDefault();int count=0;
        try{
            for(Locale global:new Locale[]{Locale.US,Locale.JAPAN,Locale.FRANCE}){
                Locale.setDefault(global);
                String date=SpanishDateFormatter.format(Instant.parse("2026-10-29T18:00:00Z"),ZoneId.of("America/Mexico_City"));
                if(!date.equals("jueves 29 de octubre de 2026"))throw new AssertionError(date);count++;
            }
            String west=SpanishDateFormatter.format(Instant.parse("2026-10-30T02:00:00Z"),ZoneId.of("America/Mexico_City"));
            String east=SpanishDateFormatter.format(Instant.parse("2026-10-30T02:00:00Z"),ZoneId.of("Asia/Tokyo"));
            if(!west.startsWith("jueves 29")||!east.startsWith("viernes 30"))throw new AssertionError("Local calendar day ignored");count++;
            String leap=SpanishDateFormatter.format(Instant.parse("2028-02-29T12:00:00Z"),ZoneId.of("UTC"));
            if(!leap.equals("martes 29 de febrero de 2028"))throw new AssertionError(leap);count++;
        }finally{Locale.setDefault(previous);}
        System.out.println("SpanishDateFormatter: "+count+" checks passed, independent of global locale");
    }
}
