package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.tvdb.api.model.Actor
import org.opensilk.tvdb.api.model.Banner
import org.opensilk.tvdb.api.model.Episode
import org.opensilk.tvdb.api.model.Series
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/19/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class DatabaseClientTest {

    lateinit var mClient: DatabaseClient
    lateinit var mProvider: DatabaseProvider

    @Before
    fun setupProvider() {
        mProvider = DatabaseProvider()
        mProvider.mDatabase = Database(RuntimeEnvironment.application)
        mProvider.mUris = DatabaseUris("foo.authority")

        mClient = DatabaseClient(RuntimeEnvironment.application,
                DatabaseUris("foo.authority"), Uri.parse("http://tvdb.foo"))
        mClient.mResolver = object: ContentResolverGlue {
            override fun insert(uri: Uri, values: ContentValues): Uri? {
                return mProvider.insert(uri, values)
            }

            override fun bulkInsert(uri: Uri, values: Array<ContentValues?>): Int {
                return mProvider.bulkInsert(uri, values)
            }

            override fun update(uri: Uri, values: ContentValues, where: String?, selectionArgs: Array<String>?): Int {
                return mProvider.update(uri, values, where, selectionArgs)
            }

            override fun query(uri: Uri, projection: Array<String>?, selection: String?,
                               selectionArgs: Array<String>?, sortOrder: String?,
                               cancellationSignal: CancellationSignal?): Cursor? {
                return mProvider.query(uri, projection, selection, selectionArgs, sortOrder, cancellationSignal)
            }

            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
                return mProvider.delete(uri, selection, selectionArgs)
            }
        }
    }

    @Test
    fun testTVDBBanner() {
        val banner = Banner(1, "/foo", "series", "1920x1080", 1.0f, 2, "/thumb", 2)
        mClient.tvdb.insertTvBanner(3, banner)
        val list = mClient.tvdb.getTvBanners(3).toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        val b = list[0]
        assertThat(EqualsBuilder.reflectionEquals(banner, b)).isTrue()
    }

    @Test
    fun testTVDBBanner_season() {
        val banner = Banner(1, "/1", "season", "season", 1.1f, 2, "/thumb1", 1)
        val banner2 = Banner(2, "/2", "series", "1920x1080", 1.2f, 2, "/thumb2", 1)
        mClient.tvdb.insertTvBanner(3, banner)
        mClient.tvdb.insertTvBanner(3, banner2)
        val list = mClient.tvdb.getTvBanners(3, 1).toBlocking().first()
        assertThat(list).isNotNull()
        assertThat(EqualsBuilder.reflectionEquals(banner, list)).isTrue()
    }

    @Test
    fun testTVSeries() {
        val series = Series(1, "foo", "this overview", "/foo", "/poster", "2009")
        mClient.tvdb.insertTvSeries(series)
        val returned = mClient.tvdb.getTvSeries().toBlocking().first()
        assertThat(returned).isNotNull()
        assertThat(EqualsBuilder.reflectionEquals(series, returned)).isTrue()
    }

    @Test
    fun testTVEpisode(){
        val episode = Episode(1, "name", "2009", "an overview", 1, 1, 3, 3)
        mClient.tvdb.insertTvEpisode(episode)
        val ret = mClient.tvdb.getTvEpisodes().toBlocking().first()
        assertThat(ret).isNotNull()
        assertThat(EqualsBuilder.reflectionEquals(episode, ret)).isTrue()
    }

    @Test
    fun testTVActor() {
        val actor = Actor(1, "name", "role", 1, "/img")
        mClient.tvdb.insertTvActor(1, actor)
        val ret = mClient.tvdb.getTvActors(1).toBlocking().first()
        assertThat(ret).isNotNull()
        assertThat(EqualsBuilder.reflectionEquals(actor, ret)).isTrue()
    }
}