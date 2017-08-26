package org.opensilk.video.telly

import android.content.Intent
import android.support.test.espresso.Espresso
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions
import android.support.test.espresso.matcher.ViewMatchers
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.getMediaIdExtra
import org.opensilk.media.putMediaIdExtra
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.opensilk.video.EXTRA_PLAY_WHEN_READY

/**
 * Created by drew on 6/7/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaybackActivityUITest {

    @Rule @JvmField
    val mActivity = object: ActivityTestRule<PlaybackActivity>(PlaybackActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return Intent().putMediaIdExtra(upnpVideo_folder_1_no_association().id)
                    .putExtra(EXTRA_PLAY_WHEN_READY, false)
        }
    }

    @Test
    fun test_activityStarts() {
        val mediaId = mActivity.activity.intent.getMediaIdExtra()
        assertThat(mediaId).isEqualTo(upnpVideo_folder_1_no_association().id.json)
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