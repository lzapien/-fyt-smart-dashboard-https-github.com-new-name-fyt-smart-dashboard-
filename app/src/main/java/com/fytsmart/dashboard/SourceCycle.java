package com.fytsmart.dashboard;

import java.util.ArrayList;
import java.util.List;

/** Pure ordering policy; availability is checked by SourceManager at request time. */
final class SourceCycle {
    static final class App {
        final String name;
        final String packageName;
        final boolean enabled;

        App(String name, String packageName, boolean enabled) {
            this.name = name;
            this.packageName = packageName;
            this.enabled = enabled;
        }
    }

    static List<App> candidates(List<App> apps, String lastSelected, boolean wrap) {
        int start = 0;
        for (int i = 0; i < apps.size(); i++) {
            if (apps.get(i).packageName.equals(lastSelected)) {
                start = i + 1;
                break;
            }
        }
        List<App> result = new ArrayList<>();
        int count = wrap ? apps.size() : apps.size() - start;
        for (int i = 0; i < count; i++) {
            App app = apps.get((start + i) % apps.size());
            if (app.enabled) result.add(app);
        }
        return result;
    }
}
