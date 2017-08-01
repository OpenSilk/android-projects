package org.opensilk.video.telly

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.assertj.core.api.Java6Assertions.assertThat
import org.opensilk.media.toMediaItem

/**
 * Created by drew on 6/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaItemListPresenterTest {

    lateinit var mPresenter: MediaItemListPresenter
    lateinit var mViewHolder: MediaItemListPresenter.ViewHolder

    @Before
    fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mPresenter = MediaItemListPresenter()
        mViewHolder = mPresenter.onCreateViewHolder(FrameLayout(activity)) as MediaItemListPresenter.ViewHolder
    }

    @Test
    fun test_onBindViewHolder() {
        val item = testUpnpVideoMetas()[0].toMediaItem()
        mPresenter.onBindViewHolder(mViewHolder, item)
        val b = mViewHolder.binding
        b.executePendingBindings()
        assertThat(b.title.text).isEqualTo(item.description.title)
        assertThat(b.subtitle.text).isEqualTo(item.description.subtitle)
        //TODO how to test icon
        assertThat(b.progress.visibility).isEqualTo(View.GONE)
    }
}