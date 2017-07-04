package org.opensilk.video.telly

import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.support.test.espresso.Espresso
import android.support.test.espresso.ViewAction
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.idling.CountingIdlingResource
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.After
import org.junit.Before

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
                    .putExtra(EXTRA_PLAY_WHEN_READY, false)
        }
    }

    @Test
    fun test_activityStarts() {
        val item = mActivity.activity.intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM)
        assertThat(item.mediaId).isEqualTo(testUpnpVideoItem().mediaId)
    }

    @Test
    fun test_action_captions_btn_hides_subtitles() {
        Espresso.onView(ViewMatchers.withId(R.id.subtitles)).check(ViewAssertions
                .matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

        Espresso.onView(ViewMatchers.withId(R.id.action_captions)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.subtitles)).check(ViewAssertions
                .matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.INVISIBLE)))

        Espresso.onView(ViewMatchers.withId(R.id.action_captions)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.subtitles)).check(ViewAssertions
                .matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))
    }

}