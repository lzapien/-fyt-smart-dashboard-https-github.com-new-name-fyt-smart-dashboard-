package com.fytsmart.dashboard;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** API 21+ session callbacks only. No Media3 dependency, service binding or OEM IPC. */
final class MediaSessionCoordinator implements AutoCloseable {
    interface Listener {
        void onMedia(MediaController controller, MediaMetadata metadata, PlaybackState state,
                     String displayedPackage, boolean artworkChanged);
        void onAccessRequired(boolean required);
    }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final MediaSessionManager manager;
    private final ComponentName notificationListener;
    private final Listener listener;
    private final Map<MediaSession.Token, Entry> entries = new LinkedHashMap<>();
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionsChanged = this::reconcile;
    private boolean active, registered, closed;
    private String selectedId, preferredPackage;
    private long sequence;

    MediaSessionCoordinator(Context context, Listener listener) {
        manager = (MediaSessionManager)context.getSystemService(Context.MEDIA_SESSION_SERVICE);
        notificationListener = new ComponentName(context, MediaListener.class);
        this.listener = listener;
    }
    void setPreferredPackage(String pkg) {
        if (pkg != null && !MediaSources.supported(pkg)) return;
        preferredPackage = pkg;
        publish(null, true);
    }
    void start() {
        if (closed || active) return;
        active = true;
        try {
            if (manager == null) { listener.onAccessRequired(false); publish(null, true); return; }
            manager.addOnActiveSessionsChangedListener(sessionsChanged, notificationListener, main);
            registered = true;
            refresh();
        } catch (SecurityException denied) { denied(); }
        catch (RuntimeException error) {
            Log.w("FYT_Media", "Cannot register session listener", error); stop();
            listener.onMedia(null, null, null, preferredPackage, true);
        }
    }
    void refresh() {
        if (!active || manager == null) return;
        try {
            reconcile(manager.getActiveSessions(notificationListener));
            listener.onAccessRequired(false);
        } catch (SecurityException denied) { denied(); }
        catch (RuntimeException error) {
            Log.w("FYT_Media", "Session discovery failed", error);
            clearEntries(); selectedId = null;
            listener.onMedia(null, null, null, preferredPackage, true);
        }
    }
    private void denied() {
        clearEntries(); selectedId = null;
        listener.onAccessRequired(true);
        listener.onMedia(null, null, null, preferredPackage, true);
    }
    private void reconcile(List<MediaController> controllers) {
        if (!active) return;
        Map<MediaSession.Token, MediaController> available = new LinkedHashMap<>();
        if (controllers != null) for (MediaController controller : controllers) {
            if (controller != null && MediaSources.supported(controller.getPackageName()))
                available.put(controller.getSessionToken(), controller);
        }
        for (MediaSession.Token token : new ArrayList<>(entries.keySet())) {
            if (!available.containsKey(token)) remove(token);
        }
        for (Map.Entry<MediaSession.Token, MediaController> candidate : available.entrySet()) {
            if (entries.containsKey(candidate.getKey())) continue;
            try {
                Entry entry = new Entry(candidate.getValue());
                entries.put(candidate.getKey(), entry);
                entry.controller.registerCallback(entry.callback, main);
                entry.metadata = entry.controller.getMetadata();
                entry.state = entry.controller.getPlaybackState();
            } catch (RuntimeException unavailable) {
                remove(candidate.getKey());
                Log.w("FYT_Media", "Session unavailable", unavailable);
            }
        }
        publish(null, false);
    }
    private final class Entry {
        final MediaController controller;
        final String id;
        MediaMetadata metadata;
        PlaybackState state;
        final MediaController.Callback callback = new MediaController.Callback() {
            @Override public void onMetadataChanged(MediaMetadata value) {
                if (!current()) return;
                metadata = value; publish(Entry.this, true);
            }
            @Override public void onPlaybackStateChanged(PlaybackState value) {
                if (!current()) return;
                state = value; publish(Entry.this, false);
            }
            @Override public void onSessionDestroyed() {
                if (!current()) return;
                remove(controller.getSessionToken()); publish(null, false);
            }
        };
        Entry(MediaController controller) {
            this.controller = controller;
            id = controller.getPackageName() + ":" + (++sequence);
        }
        boolean current() { return active && entries.get(controller.getSessionToken()) == this; }
    }
    private void publish(Entry changed, boolean artworkChanged) {
        if (!active) return;
        List<MediaSelectionPolicy.Candidate> candidates = new ArrayList<>();
        for (Entry entry : entries.values()) {
            PlaybackState state = entry.state;
            int value = state == null ? PlaybackState.STATE_NONE : state.getState();
            int priority = value == PlaybackState.STATE_PLAYING ? 3
                    : value == PlaybackState.STATE_BUFFERING || value == PlaybackState.STATE_CONNECTING ? 2
                    : value == PlaybackState.STATE_PAUSED ? 1 : 0;
            candidates.add(new MediaSelectionPolicy.Candidate(entry.id, entry.controller.getPackageName(),
                    priority, state == null ? 0 : state.getLastPositionUpdateTime()));
        }
        String next = MediaSelectionPolicy.choose(candidates, selectedId, preferredPackage);
        boolean sourceChanged = selectedId == null ? next != null : !selectedId.equals(next);
        selectedId = next;
        for (Entry entry : entries.values()) if (entry.id.equals(next)) {
            listener.onMedia(entry.controller, entry.metadata, entry.state, entry.controller.getPackageName(),
                    sourceChanged || (artworkChanged && (changed == null || changed == entry)));
            return;
        }
        listener.onMedia(null, null, null, preferredPackage, true);
    }
    private void remove(MediaSession.Token token) {
        Entry entry = entries.remove(token);
        if (entry != null) try { entry.controller.unregisterCallback(entry.callback); }
        catch (RuntimeException ignored) {}
    }
    private void clearEntries() {
        for (MediaSession.Token token : new ArrayList<>(entries.keySet())) remove(token);
    }
    void stop() {
        active = false;
        if (registered && manager != null) try { manager.removeOnActiveSessionsChangedListener(sessionsChanged); }
        catch (RuntimeException ignored) {}
        registered = false; clearEntries(); selectedId = null; main.removeCallbacksAndMessages(null);
    }
    @Override public void close() { stop(); closed = true; }
}
