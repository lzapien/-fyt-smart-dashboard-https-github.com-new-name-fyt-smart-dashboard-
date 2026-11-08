package com.fytsmart.dashboard;
public final class WeatherPresentationTest {
    public static void main(String[] args) {
        check("☀  24 °C  ·  Despejado","☀","24°C","Despejado");
        check("☾  -3 °C  ·  Despejado","☾","-3°C","Despejado");
        check("☂  0 °C  ·  Lluvia","☂","0°C","Lluvia");
        check("☁  18 °C  ·  Nublado","☁","18°C","Nublado");
        check(null,"◌","-- °C","Pendiente de ubicación");
        check("Clima no disponible","C","-- °C","Clima no disponible");
        System.out.println("WeatherPresentation: 6 checks passed");
    }
    static void check(String value,String icon,String temperature,String condition) {
        WeatherPresentation p=WeatherPresentation.parse(value);
        if(!icon.equals(p.icon)||!temperature.equals(p.temperature)||!condition.equals(p.condition))throw new AssertionError(value);
    }
}
