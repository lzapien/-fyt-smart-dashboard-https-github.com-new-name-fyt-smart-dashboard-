package com.fytsmart.dashboard;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Fixed language, system-provided instant/time zone. No change to Android's locale or clock. */
final class SpanishDateFormatter {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy",new Locale("es","MX"));
    static String format(Instant instant,ZoneId zone){return FORMAT.format(instant.atZone(zone));}
}
