package org.opensilk.video.telly

import android.content.Intent
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import org.assertj.core.api.Assertions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Created by drew on 6/1/17.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FolderActivityTest {

    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<FolderActivity>(FolderActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return super.getActivityIntent().putExtra(EXTRA_MEDIAITEM, getTestUpnpDeviceItem())
        }
    }

    @Test
    fun testMediaItemIsSet() {
        val frag = mActivityRule.activity.fragmentManager.findFragmentById(R.id.folder_browse_fragment) as FolderFragment
        Assertions.assertThat(frag.mMediaItem.mediaId).isEqualTo(getTestUpnpDeviceItem().mediaId)
    }

}