package org.opensilk.video.telly

import android.content.Intent
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.getMediaIdExtra
import org.opensilk.media.putMediaIdExtra
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association

/**
 * Created by drew on 6/4/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DetailActivityUITest {

    val mItem = upnpVideo_folder_1_no_association()
    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<DetailActivity>(DetailActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return Intent().putMediaIdExtra(mItem.id)
        }
    }

    @Test
    fun activity_starts() {
        assertThat(mActivityRule.activity.intent.getMediaIdExtra()).isEqualTo(mItem.id)
        mActivityRule.finishActivity()
    }

}