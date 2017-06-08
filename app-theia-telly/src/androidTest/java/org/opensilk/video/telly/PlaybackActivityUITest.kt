package org.opensilk.video.telly

import android.content.Intent
import android.media.browse.MediaBrowser
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.assertj.core.api.Java6Assertions.assertThat

/**
 * Created by drew on 6/7/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaybackActivityUITest {

    @Rule @JvmField
    val mActivity = object: ActivityTestRule<PlaybackActivity>(PlaybackActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return super.getActivityIntent().putExtra(EXTRA_MEDIAITEM, testUpnpVideoItem())
        }
    }

    @Test
    fun test_activityStarts() {
        val item = mActivity.activity.intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM)
        assertThat(item.mediaId).isEqualTo(testUpnpVideoItem().mediaId)
    }
}