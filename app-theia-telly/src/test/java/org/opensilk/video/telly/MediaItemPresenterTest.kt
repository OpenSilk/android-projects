package org.opensilk.video.telly

import android.app.Activity
import android.widget.FrameLayout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.opensilk.media.toMediaItem
import org.robolectric.Robolectric

/**
 * Created by drew on 6/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaItemPresenterTest {

    lateinit var mPresenter: MediaItemPresenter
    lateinit var mViewHolder: MediaItemPresenter.ViewHolder

    @Before
    fun setup() {
        mPresenter = MediaItemPresenter()
        val actvity = Robolectric.setupActivity(Activity::class.java)
        mViewHolder = mPresenter.onCreateViewHolder(FrameLayout(actvity)) as MediaItemPresenter.ViewHolder
    }

    @Test
    fun test_onBindViewHolder() {
        val item = upnpVideo_folder_1_no_association().toMediaItem()
        mPresenter.onBindViewHolder(mViewHolder, item)
        val view = mViewHolder.view as MediaItemImageCardView
        assertThat(view.titleText).isEqualTo(item.description.title)
        assertThat(view.contentText).isEqualTo(item.description.subtitle)
        //TODO how to test icon
    }
}