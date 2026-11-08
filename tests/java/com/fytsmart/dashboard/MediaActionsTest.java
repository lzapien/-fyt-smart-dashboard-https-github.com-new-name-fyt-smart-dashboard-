package com.fytsmart.dashboard;

import static android.media.session.PlaybackState.*;

/** Exercises only inlined Android constants and pure Java logic; no Android stubs execute. */
public final class MediaActionsTest {
    private static int passed;
    private static void check(long expected,long actual) {
        if(expected!=actual)throw new AssertionError(expected+" != "+actual);passed++;
    }
    public static void main(String[] args) {
        check(0,MediaActions.command(0,STATE_PLAYING,0));
        check(ACTION_PAUSE,MediaActions.command(0,STATE_PLAYING,ACTION_PLAY|ACTION_PAUSE));
        check(ACTION_PLAY,MediaActions.command(0,STATE_PAUSED,ACTION_PLAY|ACTION_PAUSE));
        check(ACTION_PLAY_PAUSE,MediaActions.command(0,STATE_PLAYING,ACTION_PLAY_PAUSE));
        check(ACTION_PLAY_PAUSE,MediaActions.command(0,STATE_PAUSED,ACTION_PLAY_PAUSE));
        check(0,MediaActions.command(0,STATE_PLAYING,ACTION_STOP));
        check(0,MediaActions.command(-1,STATE_PLAYING,ACTION_SKIP_TO_NEXT));
        check(ACTION_SKIP_TO_NEXT,MediaActions.command(1,STATE_PLAYING,ACTION_SKIP_TO_NEXT));
        check(ACTION_SKIP_TO_PREVIOUS,MediaActions.command(-1,STATE_PLAYING,ACTION_SKIP_TO_PREVIOUS));
        check(ACTION_PAUSE,MediaActions.command(0,STATE_BUFFERING,ACTION_PAUSE));
        check(ACTION_PLAY_PAUSE,MediaActions.command(0,STATE_CONNECTING,ACTION_PLAY_PAUSE));
        check(0,MediaActions.command(0,STATE_PLAYING,ACTION_PLAY));
        check(0,MediaActions.command(0,STATE_PAUSED,ACTION_PAUSE));
        check(ACTION_PLAY,MediaActions.command(0,STATE_STOPPED,ACTION_PLAY));
        System.out.println("MediaActions: "+passed+" checks passed; unsupported/STOP is never treated as mute");
    }
}
