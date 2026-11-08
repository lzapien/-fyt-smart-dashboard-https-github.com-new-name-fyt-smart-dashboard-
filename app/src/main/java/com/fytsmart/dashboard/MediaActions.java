package com.fytsmart.dashboard;

import static android.media.session.PlaybackState.*;

/** Pure decision over Android's public constant values, without sending a command. */
final class MediaActions {
    private MediaActions() {}
    static long playPauseAction(int state, long actions) {
        boolean active = state == STATE_PLAYING || state == STATE_BUFFERING || state == STATE_CONNECTING;
        long direct = active ? ACTION_PAUSE : ACTION_PLAY;
        return (actions & direct) != 0 ? direct : actions & ACTION_PLAY_PAUSE;
    }
    static long command(int direction, int state, long actions) {
        long requested = direction < 0 ? ACTION_SKIP_TO_PREVIOUS
                : direction > 0 ? ACTION_SKIP_TO_NEXT : playPauseAction(state, actions);
        return (requested & actions) != 0 ? requested : 0;
    }
}
