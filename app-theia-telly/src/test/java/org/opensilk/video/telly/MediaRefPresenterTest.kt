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
import org.robolectric.Robolectric

/**
 * Created by drew on 6/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefPresenterTest {

    lateinit var mPresenter: MediaRefPresenter
    lateinit var mViewHolder: MediaRefPresenter.ViewHolder

    @Before
    fun setup() {
        mPresenter = MediaRefPresenter()
        val actvity = Robolectric.setupActivity(Activity::class.java)
        mViewHolder = mPresenter.onCreateViewHolder(FrameLayout(actvity)) as MediaRefPresenter.ViewHolder
    }

    @Test
    fun test_onBindViewHolder() {
        val item = upnpVideo_folder_1_no_association()
        mPresenter.onBindViewHolder(mViewHolder, item)
        val view = mViewHolder.view as MediaDescImageCardView
        assertThat(view.titleText).isEqualTo(item.meta.title)
        assertThat(view.contentText).isEqualTo(item.meta.subtitle)
        //TODO how to test icon
    }
}