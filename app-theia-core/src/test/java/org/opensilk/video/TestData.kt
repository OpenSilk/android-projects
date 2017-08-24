package org.opensilk.video

import android.net.Uri
import org.opensilk.media.*

/**
 * Created by drew on 8/6/17.
 */

const val TVDB_BANNER_ROOT = "http://tvdb.foo"

fun upnpDevices(): List<UpnpDeviceRef> {
    return listOf(
            UpnpDeviceRef(
                    id = UpnpDeviceId("foo0"),
                    meta = UpnpDeviceMeta(
                            title = "Foo Server",
                            subtitle = "Mady by Foo",
                            artworkUri =  Uri.parse("http://foo.com/icon.jpg"),
                            updateId =  1002
                    )
            ),
            UpnpDeviceRef(
                    id = UpnpDeviceId("foo1"),
                    meta = UpnpDeviceMeta(
                            title = "Foo Server"
                    )
            )
    )
}

fun upnpFolders(): List<UpnpFolderRef> {
    val list = ArrayList<UpnpFolderRef>()
    for (ii in 1..10) {
        list.add(UpnpFolderRef(
                id = UpnpFolderId("foo0", UPNP_ROOT_ID, "$ii"),
                meta = UpnpFolderMeta(
                        title = "Folder $ii"
                )
        ))
    }
    return list
}

fun upnpVideo_folder_1_no_association(): UpnpVideoRef {
    return UpnpVideoRef(
            id = UpnpVideoId("foo0", "1", "1.1"),
            tvEpisodeId = null,
            movieId =  null,
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
            id = UpnpVideoId("foo0", "2", "2.1"),
            tvEpisodeId = ep.id,
            movieId =  null,
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
            id = UpnpVideoId("foo0", "3", "3.1"),
            tvEpisodeId = null,
            movieId = movie().id,
            meta = UpnpVideoMeta(
                    title = movie().meta.title,
                    originalTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/3.01.mp4"),
                    mimeType = "video/mp4"
            )
    )
}

fun tvSeries(): TvSeriesRef {
    return TvSeriesRef(
            id = TvSeriesId(1),
            meta = TvSeriesMeta(
                    title = "Series 1"
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
                    posterPath = "poster",
                    backdropPath = "backdrop"
            )
    )
}

fun movie(): MovieRef {
    return MovieRef(
            id = MovieId(1),
            meta = MovieMeta(
                    title = "Movie 1",
                    overview = "Movie Overview"
            )
    )
}