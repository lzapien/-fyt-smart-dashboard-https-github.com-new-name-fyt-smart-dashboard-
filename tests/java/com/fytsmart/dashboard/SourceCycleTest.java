package com.fytsmart.dashboard;

import java.util.List;
import java.util.stream.Collectors;

/** Standalone JVM checks; no Android runtime or new Gradle dependencies. */
public final class SourceCycleTest {
    private static int passed;
    private static SourceCycle.App app(String pkg, boolean enabled) {
        return new SourceCycle.App(pkg, pkg, enabled);
    }
    private static void check(String expected, List<SourceCycle.App> result) {
        String actual = result.stream().map(a -> a.packageName).collect(Collectors.joining(","));
        if (!expected.equals(actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
        passed++;
    }
    public static void main(String[] args) {
        List<SourceCycle.App> apps = List.of(app("radio", true), app("amazon", true), app("waze", true));
        check("radio,amazon,waze", SourceCycle.candidates(apps, null, true));
        check("amazon,waze,radio", SourceCycle.candidates(apps, "radio", true));
        check("radio,amazon,waze", SourceCycle.candidates(apps, "waze", true));
        check("radio,waze", SourceCycle.candidates(List.of(app("radio", true), app("amazon", false), app("waze", true)), null, true));
        check("", SourceCycle.candidates(List.of(), null, true));
        check("", SourceCycle.candidates(List.of(app("radio", false)), null, true));
        check("radio", SourceCycle.candidates(List.of(app("radio", true)), "radio", true));
        check("", SourceCycle.candidates(apps, "waze", false));
        check("amazon,waze", SourceCycle.candidates(apps, "radio", false));
        check("radio,amazon,waze", SourceCycle.candidates(apps, "removed", false));
        check("waze,radio,amazon", SourceCycle.candidates(List.of(app("waze", true), app("radio", true), app("amazon", true)), null, true));
        check("", SourceCycle.candidates(List.of(), null, false));
        System.out.println("SourceCycle: " + passed + " checks passed");
    }
}
