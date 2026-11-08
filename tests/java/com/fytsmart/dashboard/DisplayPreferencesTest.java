package com.fytsmart.dashboard;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/** Tests the real preference adapter with an in-memory SharedPreferences interface, without Android. */
public final class DisplayPreferencesTest {
    private static int passed;
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message);passed++; }
    public static void main(String[] args) {
        Map<String,Object> values=new HashMap<>();Map<String,Object> pending=new HashMap<>();int[] writes={0};
        SharedPreferences.Editor editor=(SharedPreferences.Editor)Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),new Class<?>[]{SharedPreferences.Editor.class},(proxy,method,arguments)->{
                    if(method.getName().equals("putString")) { pending.put((String)arguments[0],arguments[1]);return proxy; }
                    if(method.getName().equals("apply")) { values.putAll(pending);pending.clear();writes[0]++;return null; }
                    throw new AssertionError("Unexpected preference edit: "+method.getName());
                });
        SharedPreferences preferences=(SharedPreferences)Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),new Class<?>[]{SharedPreferences.class},(proxy,method,arguments)->{
                    if(method.getName().equals("edit"))return editor;
                    if(method.getName().equals("getString")) {
                        Object value=values.getOrDefault((String)arguments[0],arguments[1]);
                        if(value!=null&&!(value instanceof String))throw new ClassCastException();
                        return value;
                    }
                    throw new AssertionError("Unexpected preference access: "+method.getName());
                });
        DisplayPreferences manager=new DisplayPreferences(preferences);
        check(manager.load()==DisplayMode.AUTO,"Existing installations default to Auto without migration writes");
        check(writes[0]==0,"Reading never writes");
        values.put(ThemeManager.PLATE,"ABC-123");values.put(ThemeManager.THEME,"halloween");values.put(ThemeManager.ANIMATIONS,false);
        values.put("gps.continuous",true);
        Map<String,Object> original=new HashMap<>(values);
        for(DisplayMode mode:DisplayMode.values()) {
            int before=writes[0];manager.save(mode);
            check(writes[0]==before+1,"Each choice saves once");
            check(new DisplayPreferences(preferences).load()==mode,"A new instance restores the choice");
            for(Map.Entry<String,Object> entry:original.entrySet())check(entry.getValue().equals(values.get(entry.getKey())),"Unrelated settings preserved: "+entry.getKey());
        }
        values.put(DisplayPreferences.MODE,17);
        check(manager.load()==DisplayMode.AUTO,"Wrong stored type falls back to Auto");
        values.put(DisplayPreferences.MODE,"unknown");
        check(manager.load()==DisplayMode.AUTO,"Unknown stored ID falls back to Auto");
        check(writes[0]==3,"Fallback does not rewrite or clear private preferences");
        check(!DisplayPreferences.FILE.equals(ThemeManager.FILE)&&!DisplayPreferences.FILE.equals("dashboard_operation"),"Display colors use their own private preference file");
        System.out.println("DisplayPreferences: "+passed+" checks passed; in-memory storage contract, no device data touched");
    }
}
