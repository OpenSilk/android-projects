package org.opensilk.video

import android.content.ContentResolver
import android.content.pm.ProviderInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.testdata.TVDB_BANNER_ROOT
import org.opensilk.tvdb.api.model.Token
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, application = TestApp::class)
class DatabaseClientTest {

    lateinit var mClient: VideoAppDAO
    lateinit var mProvider: VideoAppProvider
    lateinit var mResolver: ContentResolver

    @Before
    fun setupProvider() {
        mProvider = VideoAppProvider()

        val providerInfo = ProviderInfo()
        providerInfo.authority = "foo.authority"
        mProvider = Robolectric.buildContentProvider(VideoAppProvider::class.java).create(providerInfo).get()
        mProvider.mDatabase = VideoAppDB(RuntimeEnvironment.application)
        mProvider.mUris = VideoAppDBUris("foo.authority")

        mResolver = RuntimeEnvironment.application.contentResolver

        mClient = VideoAppDAO(mResolver, VideoAppDBUris("foo.authority"), TVDB_BANNER_ROOT)
    }

    @After
    fun teardown() {
        Robolectric.reset()
        mProvider.mDatabase.close()
    }

    @Test
    fun TV_setLastUpdate() {
        mClient.setTvLastUpdate(11111)
        val ret = mClient.getTvLastUpdate().blockingGet()
        assertThat(ret).isEqualTo(11111)
    }

    @Test
    fun TV_setToken() {
        val tok = Token("foounoetu")
        mClient.setTvToken(tok)
        val ret = mClient.getTvToken().blockingGet()
        assertThat(ret).isEqualTo(tok)
    }

    @Test
    fun movie_image_base_url_returns_same_as_added() {
        val url = "http://foo.com/foo"
        mClient.setMovieImageBaseUrl(url)
        assertThat(mClient.getMovieImageBaseUrl()).isEqualTo(url)
    }
}