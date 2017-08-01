package org.opensilk.video.telly

import android.content.Intent
import android.media.browse.MediaBrowser
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
class FolderActivityUITest {

    @Rule @JvmField
    val mActivityRule = object : ActivityTestRule<FolderActivity>(FolderActivity::class.java) {
        override fun getActivityIntent(): Intent {
            return super.getActivityIntent().putExtra(EXTRA_MEDIAID, testUpnpDeviceMeta().mediaId)
        }
    }

    @Test
    fun testMediaItemIsSet() {
        val frag = mActivityRule.activity.fragmentManager.findFragmentById(R.id.folder_browse_fragment) as FolderFragment
        Assertions.assertThat(frag.arguments.getString(EXTRA_MEDIAID)).isEqualTo(testUpnpDeviceMeta().mediaId)
    }

    @Test
    fun testItemsAreLoaded() {
        val frag = mActivityRule.activity.fragmentManager.findFragmentById(R.id.folder_browse_fragment) as FolderFragment
        Assertions.assertThat((frag.mFolderAdapter.get(0) as MediaBrowser.MediaItem).mediaId)
                .isEqualTo(testUpnpFolderMetas()[0].mediaId)
    }
}