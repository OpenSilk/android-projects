package org.opensilk.media.testdata

import android.net.Uri
import org.opensilk.media.*

/**
 * Created by drew on 8/6/17.
 */

const val TVDB_BANNER_ROOT = "http://tvdb.foo"
const val MOVIEDB_BANNER_ROOT = "http://movie.foo"

fun upnpDevice_all_meta(): UpnpDeviceRef {
    return UpnpDeviceRef(
            id = UpnpDeviceId(deviceId = "foo0"),
            meta = UpnpDeviceMeta(
                    title = "Foo Server",
                    subtitle = "Mady by Foo",
                    artworkUri = Uri.parse("http://foo.com/icon.jpg"),
                    updateId = 1002
            )
    )
}

fun upnpDevice_minimal_meta(): UpnpDeviceRef {
    return UpnpDeviceRef(
            id = UpnpDeviceId(deviceId = "foo1"),
            meta = UpnpDeviceMeta(
                    title = "Foo Server"
            )
    )
}

fun upnpFolders(): List<UpnpFolderRef> {
    val list = ArrayList<UpnpFolderRef>()
    (1..10).mapTo(list) {
        UpnpFolderRef(
                id = UpnpFolderId(deviceId = "foo0", parentId = "0", containerId = "$it"),
                meta = UpnpFolderMeta(
                        title = "Folder $it"
                )
        )
    }
    return list
}

fun upnpVideo_folder_1_no_association(): UpnpVideoRef {
    return UpnpVideoRef(
            id = UpnpVideoId(deviceId = "foo0", parentId = "1", itemId = "1.1"),
            tvEpisodeId = null,
            movieId = null,
            meta = UpnpVideoMeta(
                    title = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/1.01.mp4"),
                    mimeType = "video/mp4"
            )
    )
}

fun upnpVideo_folder_2_episode_id(): UpnpVideoRef {
    val ser = tvSeries()
    val ep = tvEpisode()
    return UpnpVideoRef(
            id = UpnpVideoId(deviceId = "foo0", parentId = "2", itemId = "2.1"),
            tvEpisodeId = ep.id,
            movieId = null,
            meta = UpnpVideoMeta(
                    title = ep.meta.title,
                    subtitle = "${ser.meta.title} - S0${ep.meta.seasonNumber}E0${ep.meta.episodeNumber}",
                    originalTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/2.01.mp4"),
                    mimeType = "video/mp4",
                    artworkUri = Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(ep.meta.posterPath).build(),
                    backdropUri = Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(ep.meta.backdropPath).build()
            )
    )
}

fun upnpVideo_folder_3_movie_id(): UpnpVideoRef {
    return UpnpVideoRef(
            id = UpnpVideoId(deviceId = "foo0",parentId = "3",itemId =  "3.1"),
            tvEpisodeId = null,
            movieId =  movie().id,
            meta = UpnpVideoMeta(
                    title = movie().meta.title,
                    originalTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/3.01.mp4"),
                    artworkUri = Uri.parse(MOVIEDB_BANNER_ROOT).buildUpon().appendPath(movie().meta.posterPath).build(),
                    backdropUri= Uri.parse(MOVIEDB_BANNER_ROOT).buildUpon().appendPath(movie().meta.backdropPath).build(),
                    mimeType = "video/mp4"
            )
    )
}

fun tvSeries(): TvSeriesRef {
    return TvSeriesRef(
            id = TvSeriesId(1),
            meta = TvSeriesMeta(
                    title = "Series 1",
                    overview = "Series overview",
                    posterPath = "s_poster.jpg",
                    backdropPath = "s_backdrop.jpg"
            )
    )
}

fun tvEpisode(): TvEpisodeRef {
    return TvEpisodeRef(
            id = TvEpisodeId(1, 1),
            meta = TvEpisodeMeta(
                    title = "Episode 1",
                    overview = "Episode Overview",
                    episodeNumber = 1,
                    seasonNumber = 1,
                    posterPath = "t_poster.jpg",
                    backdropPath = "t_backdrop.jpg",
                    releaseDate = "2017-09-10"
            )
    )
}

fun movie(): MovieRef {
    return MovieRef(
            id = MovieId(1),
            meta = MovieMeta(
                    title = "Movie 1",
                    overview = "Movie Overview",
                    posterPath = "m_poster.jpg",
                    backdropPath = "m_backdrop.jpg",
                    releaseDate = "2017-03-04"
            )
    )
}