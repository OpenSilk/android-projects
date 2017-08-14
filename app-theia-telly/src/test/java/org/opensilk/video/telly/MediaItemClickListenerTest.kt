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
import org.opensilk.media.testdata.upnpDevice_all_meta
import org.opensilk.media.testdata.upnpFolders
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.opensilk.media.toMediaItem
import org.opensilk.video.EXTRA_MEDIAID
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
        val item = upnpDevice_all_meta().toMediaItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, FolderActivity::class.java))
        assertThat(intent.getStringExtra(EXTRA_MEDIAID)).isEqualTo(item.mediaId)
    }

    @Test
    fun test_OnItemClicked_FolderItem() {
        val item = upnpFolders()[0].toMediaItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, FolderActivity::class.java))
        assertThat(intent.getStringExtra(EXTRA_MEDIAID)).isEqualTo(item.mediaId)
    }

    @Test
    fun test_OnItemClicked_VideoItem() {
        val item = upnpVideo_folder_1_no_association().toMediaItem()
        mListener.onItemClicked(mItemViewHolder, item, null, null)
        val intent = Shadows.shadowOf(mActivity).nextStartedActivity
        assertThat(intent.component).isEqualTo(ComponentName(mActivity, DetailActivity::class.java))
        assertThat(intent.getStringExtra(EXTRA_MEDIAID)).isEqualTo(item.mediaId)
    }

}