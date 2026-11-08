package com.fytsmart.dashboard;

import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.util.Log;
import android.view.KeyEvent;

/** Commands are addressed to this exact advertised MediaSession, never global media keys. */
final class MediaControlDispatcher {
    private MediaControlDispatcher() {}
    static long playPauseAction(PlaybackState state) {
        if (state == null) return 0;
        return MediaActions.playPauseAction(state.getState(),state.getActions());
    }
    static boolean send(MediaController controller, int action) {
        if (controller == null || !MediaSources.supported(controller.getPackageName())) return false;
        try {
            PlaybackState state = controller.getPlaybackState();
            if (state == null) return false;
            long requested = MediaActions.command(action,state.getState(),state.getActions());
            if (requested == 0) return false;
            Log.i("FYT_Media", "Transport requested package=" + controller.getPackageName()
                    + " action=" + requested + "; audible-source change not confirmed");
            MediaController.TransportControls transport = controller.getTransportControls();
            if (requested == PlaybackState.ACTION_SKIP_TO_PREVIOUS) transport.skipToPrevious();
            else if (requested == PlaybackState.ACTION_SKIP_TO_NEXT) transport.skipToNext();
            else if (requested == PlaybackState.ACTION_PLAY) transport.play();
            else if (requested == PlaybackState.ACTION_PAUSE) transport.pause();
            else {
                boolean down = controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                boolean up = controller.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
                return down && up;
            }
            return true;
        } catch (RuntimeException error) {
            Log.w("FYT_Media", "Transport rejected by session", error); return false;
        }
    }
}
