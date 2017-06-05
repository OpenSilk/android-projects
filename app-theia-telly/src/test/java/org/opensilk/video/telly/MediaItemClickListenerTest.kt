package org.opensilk.video.telly

import android.app.Activity
import android.content.ComponentName
import android.media.browse.MediaBrowser
import android.support.v17.leanback.widget.Presenter
import android.widget.FrameLayout
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Created by drew on 6/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaItemClickListenerTest {

    lateinit var mListener: MediaItemClickListener
    lateinit var mActivity: Activity
    lateinit var mItemViewHolder: Presenter.ViewHolder

    @Before
    fun setup() {
        mListener = MediaItemClickListener()
        mActivity = Robolectric.setupActivity(Activity::class.java)
        mItemViewHolder = Presenter.ViewHolder(FrameLayout(mActivity))
    }

    @Test
    fun test_OnItemClicked_DeviceItem() {
        val item = testUpnpDeviceItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, FolderActivity::class.java))
        assertThat(intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM))
                .isEqualTo(item)
    }

    @Test
    fun test_OnItemClicked_FolderItem() {
        val item = testUpnpFolderItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, FolderActivity::class.java))
        assertThat(intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM))
                .isEqualTo(item)
    }

    @Test
    fun test_OnItemClicked_VideoItem() {
        val item = testUpnpVideoItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, DetailActivity::class.java))
        assertThat(intent.getParcelableExtra<MediaBrowser.MediaItem>(EXTRA_MEDIAITEM))
                .isEqualTo(item)
    }

}