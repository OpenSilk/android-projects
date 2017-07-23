package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import org.apache.commons.lang3.ObjectUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.*
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

            override fun notifyChange(uri: Uri, co: ContentObserver?) {
                //pass
            }
        }
    }

    @Test
    fun testUpnpVideo_multiple_add_get() {
        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))

        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")

        val meta2 = MediaMeta()
        val mid2 = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar2"))
        meta2.mediaId = mid2.toJson()
        meta2.parentMediaId = parentmid.toJson()
        meta2.mimeType = "video/mpeg"
        meta2.displayName = "my display name"
        meta2.mediaUri = Uri.parse("https://foo.com/vid.mp4")

        val uri = mClient.addUpnpVideo(meta)
        val uri2 = mClient.addUpnpVideo(meta2)
        val retrieved = mClient.getUpnpVideos(parentmid.mediaId as UpnpFolderId).toList().toBlocking().first()
        assertThat(retrieved).isNotNull
        assertThat(retrieved.size).isEqualTo(2)
        assertThat(retrieved).containsAll(listOf(meta, meta2))
    }

    @Test
    fun testUpnpVideo_single_add_get() {
        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        val parentmid = MediaRef(UPNP_FOLDER, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")
        val uri = mClient.addUpnpVideo(meta)
        val retrieved = mClient.getUpnpVideo(uri.lastPathSegment.toLong()).toBlocking().value()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved.mediaId).isEqualTo(meta.mediaId)
        //todo more assertions
    }

    @Test(expected = RuntimeException::class)
    fun testUpnpVideo_single_add_get_remove() {
        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        val parentmid = MediaRef(UPNP_FOLDER, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")
        val uri = mClient.addUpnpVideo(meta)
        mClient.removeUpnpVideo(uri.lastPathSegment.toLong())
        mClient.getUpnpVideo(uri.lastPathSegment.toLong()).toBlocking().value() //throws
    }

    @Test
    fun testUpnpVideo_duplicate_add_does_not_replace() {
        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        val parentmid = MediaRef(UPNP_FOLDER, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")
        val uri = mClient.addUpnpVideo(meta)
        val retrieved = mClient.getUpnpVideo(uri.lastPathSegment.toLong()).toBlocking().value()
        assertThat(retrieved).isNotNull()
        assertThat(retrieved.mediaId).isEqualTo(meta.mediaId)
        val uri2 = mClient.addUpnpVideo(meta)
        assertThat(uri2).isEqualTo(uri)
        val retrieved2 = mClient.getUpnpVideo(uri2.lastPathSegment.toLong()).toBlocking().value()
        assertThat(retrieved2.mediaId).isEqualTo(meta.mediaId)
    }

    @Test
    fun testUpnpFolder_add_remove_get() {
        val meta = MediaMeta()
        val mid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))
        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "0"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = MIME_TYPE_DIR
        meta.displayName = "a foo title"
        meta.artworkUri = Uri.parse("http://foo.com")
        mClient.addUpnpFolder(meta)
        val list = mClient.getUpnpFolders(parentmid.mediaId as UpnpFolderId).toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0].mediaId).isEqualTo(meta.mediaId)
        //TODO more assertions on item
        mClient.removeUpnpFolder(list[0].rowId)
        val list2 = mClient.getUpnpDevices().toList().toBlocking().first()
        assertThat(list2.size).isEqualTo(0)
    }

    @Test
    fun testUpnpDevice_add_hide_get() {
        val meta = MediaMeta()
        val mId = MediaRef(UPNP_DEVICE, UpnpDeviceId("foo"))
        meta.mediaId = mId.toJson()
        meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
        meta.displayName = "a heading"
        meta.subtitle = "a sub heading"
        meta.artworkUri = Uri.parse("http://foo.com")
        mClient.addUpnpDevice(meta)
        val list = mClient.getUpnpDevices().toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0].mediaId).isEqualTo(meta.mediaId)
        //todo more assertions on item
        mClient.hideUpnpDevice((mId.mediaId as UpnpDeviceId).deviceId)
        val list2 = mClient.getUpnpDevices().toList().toBlocking().first()
        assertThat(list2.size).isEqualTo(0)
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