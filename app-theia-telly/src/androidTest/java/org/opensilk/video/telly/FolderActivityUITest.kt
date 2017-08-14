package org.opensilk.video.telly

import android.content.Intent
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.testdata.upnpDevice_all_meta
import org.opensilk.video.EXTRA_MEDIAID

/**
 * Created by drew on 6/1/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FolderActivityUITest {

    val mDevice = upnpDevice_all_meta()
    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<FolderActivity>(FolderActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return Intent().putExtra(EXTRA_MEDIAID, mDevice.id.json)
        }
    }

    @Test
    fun activityStarts() {
        Assertions.assertThat(mActivityRule.activity.intent.getStringExtra(EXTRA_MEDIAID))
                .isEqualTo(mDevice.id.json)
    }

}