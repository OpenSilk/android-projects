package org.opensilk.media.playback;

import android.content.Context;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import rx.functions.Action1;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DefaultRendererTest {

    private Context appContext;
    private DefaultRenderer renderer;

    @Before
    public void setup() {
        appContext = InstrumentationRegistry.getTargetContext();
        renderer = new DefaultRenderer(appContext);
    }

    @After
    public void destroy() {
        renderer.destroy();
    }

    @Test
    public void testLoadPlayPause() throws Exception {
        renderer.loadMedia(Uri.parse("http://datashat.net/music_for_programming_1-datassette.mp3"),
                Collections.<String, String>emptyMap());
        renderer.play();
        while (!renderer.getMCurrentPlayer().isPlaying()) {} //wait for it to actually start
        synchronized (this) {
            wait(50);
        }
        renderer.pause();
        Assertions.assertThat(renderer.getMCurrentPlayer().isPlaying()).isFalse();
        Assertions.assertThat(renderer.getState()).isEqualTo(PlaybackState.STATE_PAUSED);
    }

    @Test
    public void testPauseCancelsPlay() throws Exception {
        renderer.loadMedia(Uri.parse("http://datashat.net/music_for_programming_1-datassette.mp3"),
                Collections.<String, String>emptyMap());
        renderer.play();
        renderer.pause();
        while (renderer.getState() == PlaybackState.STATE_BUFFERING) {}
        synchronized (this) {
            wait(50);
        }
        Assertions.assertThat(renderer.getState()).isEqualTo(PlaybackState.STATE_PAUSED);
    }

}
