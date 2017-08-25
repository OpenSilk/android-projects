package org.opensilk.video.telly

import android.app.Activity
import android.net.Uri
import android.view.View
import android.widget.FrameLayout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.assertj.core.api.Java6Assertions.assertThat
import org.opensilk.media.database.ApiHelper
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.MediaDBUris
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.robolectric.RuntimeEnvironment

/**
 * Created by drew on 6/4/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefListPresenterTest {

    lateinit var mPresenter: MediaRefListPresenter
    lateinit var mViewHolder: MediaRefListPresenter.ViewHolder

    @Before
    fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mPresenter = MediaRefListPresenter(MediaDAO(
                RuntimeEnvironment.application.contentResolver,
                MediaDBUris("foo"),
                object : ApiHelper {
                    override fun tvImagePosterUri(path: String): Uri {
                        TODO("not implemented")
                    }

                    override fun tvImageBackdropUri(path: String): Uri {
                        TODO("not implemented")
                    }

                    override fun movieImagePosterUri(path: String): Uri {
                        TODO("not implemented")
                    }

                    override fun movieImageBackdropUri(path: String): Uri {
                        TODO("not implemented")
                    }
                }
        ))
        mViewHolder = mPresenter.onCreateViewHolder(FrameLayout(activity)) as MediaRefListPresenter.ViewHolder
    }

    @Test
    fun test_onBindViewHolder() {
        val item = upnpVideo_folder_1_no_association()
        mPresenter.onBindViewHolder(mViewHolder, item)
        val b = mViewHolder.binding
        b.executePendingBindings()
        assertThat(b.title.text).isEqualTo(item.meta.title)
        assertThat(b.subtitle.text).isEqualTo(item.meta.subtitle)
        //TODO how to test icon
        assertThat(b.progress.visibility).isEqualTo(View.GONE)
    }
}