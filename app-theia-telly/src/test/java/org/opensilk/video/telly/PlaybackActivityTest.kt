package org.opensilk.video.telly

import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * Created by drew on 6/8/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class PlaybackActivityTest {

    @Test
    fun test_timeTickReceiverIsUnregisteredInOnStop() {
        val controller = Robolectric.buildActivity(PlaybackActivity::class.java,
                Intent(ACTION_PLAY).putExtra(EXTRA_MEDIAID, testUpnpVideoMetas()[0].mediaId))
        controller.create().postCreate(null).start().resume().visible()
        assertThat(Shadows.shadowOf(RuntimeEnvironment.application)
                .getReceiversForIntent(Intent(Intent.ACTION_TIME_TICK)).size).isEqualTo(1)
        controller.pause().stop()
        assertThat(Shadows.shadowOf(RuntimeEnvironment.application)
                .getReceiversForIntent(Intent(Intent.ACTION_TIME_TICK)).size).isEqualTo(0)
    }

}