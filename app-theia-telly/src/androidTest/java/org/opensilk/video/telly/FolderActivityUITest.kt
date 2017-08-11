package org.opensilk.video.telly

import android.content.Intent
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.video.EXTRA_MEDIAID
import org.opensilk.video.upnpDevices

/**
 * Created by drew on 6/1/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FolderActivityUITest {

    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<FolderActivity>(FolderActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return Intent().putExtra(EXTRA_MEDIAID, upnpDevices()[0].id.json)
        }
    }

    @Test
    fun activityStarts() {
        Assertions.assertThat(mActivityRule.activity.intent.getStringExtra(EXTRA_MEDIAID))
                .isEqualTo(upnpDevices()[0].id.json)
    }

}