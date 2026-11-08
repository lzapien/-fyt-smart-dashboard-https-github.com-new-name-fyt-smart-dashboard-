package com.fytsmart.dashboard;

import java.util.List;

/** Selection is a UI policy, not confirmation of the FYT hardware's audible source. */
final class MediaSelectionPolicy {
    static final class Candidate {
        final String id, pkg;
        final int priority;
        final long updated;
        Candidate(String id, String pkg, int priority, long updated) {
            this.id = id; this.pkg = pkg; this.priority = priority; this.updated = updated;
        }
    }
    private MediaSelectionPolicy() {}
    static String choose(List<Candidate> sessions, String previousId, String preferredPackage) {
        Candidate best = null;
        for (Candidate candidate : sessions) {
            if (!MediaSources.supported(candidate.pkg)) continue;
            // Explicit UI preference may intentionally select an app with no session.
            if (preferredPackage != null && !preferredPackage.equals(candidate.pkg)) continue;
            if (best == null || candidate.priority > best.priority
                    || (candidate.priority == best.priority && candidate.updated > best.updated)
                    || (candidate.priority == best.priority && candidate.updated == best.updated
                        && candidate.id.equals(previousId))) best = candidate;
        }
        return best == null ? null : best.id;
    }
}
