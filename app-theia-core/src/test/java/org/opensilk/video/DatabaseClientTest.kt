package org.opensilk.video

import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.media.*
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tvdb.api.model.Series
import org.opensilk.tvdb.api.model.SeriesEpisode
import org.opensilk.tvdb.api.model.Token
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
                DatabaseUris("foo.authority"), "http://tvdb.foo")
        mClient.mResolver = object: ContentResolverGlue {
            override fun insert(uri: Uri, values: ContentValues): Uri? {
                return mProvider.insert(uri, values)
            }

            override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
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

    @After
    fun teardown() {
        mProvider.mDatabase.close()
        mClient.mResolver = mock()
    }

    @Test
    fun upnpVideoOverview_movie() {

        val movie = Movie(100, "foo movie", null, "an overview 1", "2001", "/paster", "/backdrop")
        mClient.addMovie(movie)

        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))

        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")

        mClient.addUpnpVideo(meta)
        mClient.setUpnpVideoMovieId(UpnpVideoId("foo", "bar"), 100)

        assertThat(mClient.getUpnpVideoOverview(UpnpVideoId("foo", "bar")).toBlocking().value())
                .isEqualTo("an overview 1")
    }

    @Test
    fun upnpVideoOverview_tvEpisode() {
        val episode = SeriesEpisode(id = 1, episodeName = "name",
                overview = "an overview", airedSeason =  1,
                airedEpisodeNumber = 1, airedSeasonId = 1)
        mClient.addTvEpisodes(1, listOf(episode))

        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))

        val meta = MediaMeta()
        val mid = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = "video/mpeg"
        meta.displayName = "my display name"
        meta.mediaUri = Uri.parse("https://foo.com/vid.mp4")

        mClient.addUpnpVideo(meta)
        mClient.setUpnpVideoTvEpisodeId(UpnpVideoId("foo", "bar"), 1)

        assertThat(mClient.getUpnpVideoOverview(UpnpVideoId("foo", "bar")).toBlocking().value())
                .isEqualTo("an overview")
    }

    @Test
    fun testHideChildren() {
        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "0"))

        val meta = MediaMeta()
        val mid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = MIME_TYPE_DIR
        meta.title = "a foo title"

        val uri = mClient.addUpnpFolder(meta)
        assertThat(mClient.getUpnpFolders(parentmid.mediaId as UpnpFolderId)
                .count().toBlocking().first()).isEqualTo(1)

        val meta2 = MediaMeta()
        val mid2 = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "bar2"))
        meta2.mediaId = mid2.toJson()
        meta2.parentMediaId = parentmid.toJson()
        meta2.mimeType = "video/mpeg"
        meta2.displayName = "my display name"
        meta2.mediaUri = Uri.parse("https://foo.com/vid.mp4")

        val uri2 = mClient.addUpnpVideo(meta2)
        assertThat(mClient.getUpnpVideos(parentmid.mediaId as UpnpFolderId)
                .count().toBlocking().first()).isEqualTo(1)

        mClient.hideChildrenOf(parentmid.mediaId as UpnpFolderId)

        assertThat(mClient.getUpnpFolders(parentmid.mediaId as UpnpFolderId)
                .isEmpty.toBlocking().first()).isTrue()
        assertThat(mClient.getUpnpVideos(parentmid.mediaId as UpnpFolderId)
                .isEmpty.toBlocking().first()).isTrue()
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
        assertThat(retrieved.mediaId).isEqualTo(meta.mediaId)
        //todo more assertions

        val retrieved2 = mClient.getUpnpVideo(mid.mediaId as UpnpVideoId).toBlocking().value()
        assertThat(retrieved2.mediaId).isEqualTo(meta.mediaId)
    }

    @Test
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
        val retrieved = mClient.getUpnpVideo(uri.lastPathSegment.toLong())
                .onErrorReturn { null }.toBlocking().value()
        assertThat(retrieved).isNull()
    }

    @Test
    fun upnp_video_duplicate_add_changes_name() {
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
        assertThat(retrieved.displayName).isEqualTo("my display name")

        meta.displayName = "another display name"
        val uri2 = mClient.addUpnpVideo(meta)
        val retrieved2 = mClient.getUpnpVideo(uri2.lastPathSegment.toLong()).toBlocking().value()
        assertThat(retrieved2.displayName).isEqualTo("another display name")

    }

    @Test
    fun upnp_video_duplicate_add_does_not_replace() {
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
    fun UpnpFolder_add_remove_get() {
        val meta = MediaMeta()
        val mid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "bar"))
        val parentmid = MediaRef(UPNP_FOLDER, UpnpFolderId("foo", "0"))
        meta.mediaId = mid.toJson()
        meta.parentMediaId = parentmid.toJson()
        meta.mimeType = MIME_TYPE_DIR
        meta.title = "a foo title"
        meta.artworkUri = Uri.parse("http://foo.com")
        mClient.addUpnpFolder(meta)

        val list = mClient.getUpnpFolders(parentmid.mediaId as UpnpFolderId).toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0].mediaId).isEqualTo(meta.mediaId)

        val retrieved = mClient.getUpnpFolder(mid.mediaId as UpnpFolderId).toBlocking().value()
        assertThat(retrieved.mediaId).isEqualTo(meta.mediaId)

        //TODO more assertions on item
        mClient.removeUpnpFolder(list[0].rowId)
        val list2 = mClient.getUpnpDevices().toList().toBlocking().first()
        assertThat(list2.size).isEqualTo(0)
    }

    @Test
    fun upnp_device_add_after_increment_scanning_resets_value() {
        val meta = MediaMeta()
        val mId = MediaRef(UPNP_DEVICE, UpnpDeviceId("foo"))
        meta.mediaId = mId.toJson()
        meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
        meta.title = "a heading"
        meta.subtitle = "a sub heading"

        val uri = mClient.addUpnpDevice(meta)
        val changed = mClient.incrementUpnpDeviceScanning(UpnpDeviceId("foo"))
        assertThat(changed).isTrue()
        val num = mClient.getUpnpDeviceScanning(UpnpDeviceId("foo")).toBlocking().value()
        assertThat(num).isEqualTo(1)
        val uri2 = mClient.addUpnpDevice(meta)
        val num2 = mClient.getUpnpDeviceScanning(UpnpDeviceId("foo")).toBlocking().value()
        assertThat(num2).isEqualTo(0)
    }

    @Test
    fun upnp_device_increment_decrement_scanning() {
        val meta = MediaMeta()
        val mId = MediaRef(UPNP_DEVICE, UpnpDeviceId("foo"))
        meta.mediaId = mId.toJson()
        meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
        meta.title = "a heading"

        val uri = mClient.addUpnpDevice(meta)
        val changed = mClient.incrementUpnpDeviceScanning(UpnpDeviceId("foo"))
        assertThat(changed).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(UpnpDeviceId("foo")).toBlocking().value()).isEqualTo(1)
        val changed2 = mClient.incrementUpnpDeviceScanning(UpnpDeviceId("foo"))
        assertThat(changed2).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(UpnpDeviceId("foo")).toBlocking().value()).isEqualTo(2)

        val changed3 = mClient.decrementUpnpDeviceScanning(UpnpDeviceId("foo"))
        assertThat(changed3).isTrue()
        assertThat(mClient.getUpnpDeviceScanning(UpnpDeviceId("foo")).toBlocking().value()).isEqualTo(1)

    }

    @Test
    fun upnpdevice_add_twice_returns_same_id() {
        val meta = MediaMeta()
        val mId = MediaRef(UPNP_DEVICE, UpnpDeviceId("foo"))
        meta.mediaId = mId.toJson()
        meta.mimeType = MIME_TYPE_CONTENT_DIRECTORY
        meta.displayName = "a heading"
        meta.subtitle = "a sub heading"

        val uri = mClient.addUpnpDevice(meta)
        val uri2 = mClient.addUpnpDevice(meta)
        assertThat(uri2).isEqualTo(uri)
    }

    @Test
    fun UpnpDevice_add_hide_get() {
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

        val retrieved = mClient.getUpnpDevice(mId.mediaId as UpnpDeviceId).toBlocking().value()
        assertThat(retrieved.mediaId).isEqualTo(meta.mediaId)

        mClient.hideUpnpDevice((mId.mediaId as UpnpDeviceId).deviceId)
        val list2 = mClient.getUpnpDevices().toList().toBlocking().first()
        assertThat(list2.size).isEqualTo(0)
    }

    @Test
    fun TV_setLastUpdate() {
        mClient.setTvLastUpdate(11111)
        val ret = mClient.getTvLastUpdate().toBlocking().value()
        assertThat(ret).isEqualTo(11111)
    }

    @Test
    fun TV_setToken() {
        val tok = Token("foounoetu")
        mClient.setTvToken(tok)
        val ret = mClient.getTvToken().toBlocking().value()
        assertThat(ret).isEqualTo(tok)
    }

    @Test
    fun TVSeries_add_get() {
        val series = Series(id = 1, seriesName = "foo", overview =  "this overview",
                firstAired = "2009", lastUpdated = 1111)
        val uri = mClient.addTvSeries(series)
        val returned = mClient.getTvSeries(uri.lastPathSegment.toLong()).toBlocking().value()
        assertThat(returned.rowId).isEqualTo(series.id)
        assertThat(returned.title).isEqualTo(series.seriesName)
        assertThat(returned.overview).isEqualTo(series.overview)
        assertThat(returned.releaseDate).isEqualTo(series.firstAired)
        assertThat(returned.mimeType).isEqualTo(MIME_TYPE_TV_SERIES)
    }

    @Test
    fun TVEpisode_add_get(){
        val episode = SeriesEpisode(id = 1, episodeName = "name",
                firstAired =  "2009", overview = "an overview", airedSeason =  1,
                airedEpisodeNumber = 1, airedSeasonId = 1)
        mClient.addTvEpisodes(1, listOf(episode))
        val ret = mClient.getTvEpisode(episode.id).toBlocking().value()
        assertThat(ret.rowId).isEqualTo(episode.id)
        assertThat(ret.title).isEqualTo(episode.episodeName)
        assertThat(ret.releaseDate).isEqualTo(episode.firstAired)
        assertThat(ret.episodeNumber).isEqualTo(ret.episodeNumber)
        assertThat(ret.mimeType).isEqualTo(MIME_TYPE_TV_EPISODE)
    }

    @Test
    fun TV_seriesAssociation() {
        mClient.setTvSeriesAssociation("foobar", 100)
        assertThat(mClient.getTvSeriesAssociation("foobar").toBlocking().value()).isEqualTo(100)
    }

    @Test
    fun movie_movieAssociation() {
        mClient.setMovieAssociation("foo", "2001", 1002)
        assertThat(mClient.getMovieAssociation("foo", "2001").toBlocking().value()).isEqualTo(1002)
    }

    @Test
    fun movie_movieAssociation_different_year() {
        mClient.setMovieAssociation("foo", "", 1002)
        val retrieved = mClient.getMovieAssociation("foo", "2001")
                .onErrorReturn { 2939393 }.toBlocking().value()
        assertThat(retrieved).isEqualTo(2939393)
    }

    @Test
    fun movie_add_get() {
        mClient.setMovieImageBaseUrl("https://foo.com")
        val movie = Movie(100, "foo movie", null, "an overiview", "2001", "/paster", "/backdrop")
        val uri = mClient.addMovie(movie)
        val retrieved = mClient.getMovie(uri.lastPathSegment.toLong()).toBlocking().value()
        assertThat(retrieved.rowId).isEqualTo(100)
        assertThat(retrieved.title).isEqualTo(movie.title)
        assertThat(retrieved.overview).isEqualTo(movie.overview)
        assertThat(retrieved.releaseDate).isEqualTo(movie.releaseDate)
        assertThat(retrieved.mimeType).isEqualTo(MIME_TYPE_MOVIE)
        assertThat(retrieved.artworkUri).isEqualTo(mClient.makeMoviePosterUri("https://foo.com", movie.posterPath))
    }

    @Test
    fun movie_config_ops() {
        val config = TMDbConfig(TMDbConfig.Images("https://foo.com/", null, null, null, null, null, null))
        mClient.setMovieImageBaseUrl(config.images.baseUrl)
        val retrieved = mClient.getMovieImageBaseUrl()
        assertThat(retrieved).isEqualTo(config.images.baseUrl)
    }
}