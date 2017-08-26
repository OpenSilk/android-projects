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
import org.opensilk.media.putMediaIdExtra
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.opensilk.video.ACTION_PLAY

/**
 * Created by drew on 6/8/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class PlaybackActivityTest {

    @Test
    fun test_timeTickReceiverIsUnregisteredInOnStop() {
        val controller = Robolectric.buildActivity(PlaybackActivity::class.java,
                Intent(ACTION_PLAY).putMediaIdExtra(upnpVideo_folder_1_no_association().id))
        controller.create().postCreate(null).start().resume().visible()
        assertThat(Shadows.shadowOf(RuntimeEnvironment.application)
                .getReceiversForIntent(Intent(Intent.ACTION_TIME_TICK)).size).isEqualTo(1)
        controller.pause().stop()
        assertThat(Shadows.shadowOf(RuntimeEnvironment.application)
                .getReceiversForIntent(Intent(Intent.ACTION_TIME_TICK)).size).isEqualTo(0)
    }

}