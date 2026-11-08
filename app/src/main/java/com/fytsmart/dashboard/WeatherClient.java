package com.fytsmart.dashboard;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;

/** Weather lookup using Open-Meteo; no network work on the UI thread. */
public final class WeatherClient {
    public interface Callback {
        void onResult(String displayText);
        void onError();
    }
    private WeatherClient() {}

    public static void fetch(double lat, double lon, Callback cb) {
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                String endpoint = String.format(Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.5f&longitude=%.5f&current=temperature_2m,weather_code,is_day&timezone=auto",
                    lat, lon);
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("Accept", "application/json");
                if (connection.getResponseCode() != 200) throw new Exception("HTTP " + connection.getResponseCode());
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                JSONObject current = new JSONObject(json.toString()).getJSONObject("current");
                int code = current.getInt("weather_code");
                boolean day = current.optInt("is_day", 1) == 1;
                int temp = (int) Math.round(current.getDouble("temperature_2m"));
                String result = icon(code, day) + "  " + temp + " °C  ·  " + description(code);
                new Handler(Looper.getMainLooper()).post(() -> cb.onResult(result));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(cb::onError);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }, "FYT-Weather").start();
    }

    private static String icon(int code, boolean day) {
        if (code == 0) return day ? "☀" : "☾";
        if (code <= 3) return "☁";
        if (code == 45 || code == 48) return "≋";
        if (code >= 95) return "⚡";
        if (code >= 71 && code <= 86) return "❄";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "☂";
        return "☁";
    }

    private static String description(int code) {
        if (code == 0) return "Despejado";
        if (code <= 2) return "Poco nublado";
        if (code == 3) return "Nublado";
        if (code == 45 || code == 48) return "Niebla";
        if (code >= 95) return "Tormenta";
        if (code >= 71 && code <= 86) return "Nieve";
        if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82)) return "Lluvia";
        return "Clima variable";
    }
}
