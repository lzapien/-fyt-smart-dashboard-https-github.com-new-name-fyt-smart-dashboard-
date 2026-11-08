package com.fytsmart.dashboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formats the existing WeatherClient result without inventing a city or adding a provider. */
final class WeatherPresentation {
    private static final Pattern TEMPERATURE=Pattern.compile("(-?\\d+)\\s*°C");
    final String icon, temperature, condition;
    private WeatherPresentation(String icon,String temperature,String condition) {
        this.icon=icon;this.temperature=temperature;this.condition=condition;
    }
    static WeatherPresentation parse(String result) {
        String value=result==null?"":result.trim();String[] parts=value.split("·",2);
        Matcher match=TEMPERATURE.matcher(parts[0]);
        String temperature=match.find()?match.group(1)+"°C":"-- °C";
        String icon=value.isEmpty()?"◌":new String(Character.toChars(value.codePointAt(0)));
        String condition=parts.length>1?parts[1].trim():value.isEmpty()?"Pendiente de ubicación":value;
        return new WeatherPresentation(icon,temperature,condition);
    }
}
