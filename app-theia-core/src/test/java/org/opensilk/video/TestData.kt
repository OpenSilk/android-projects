package org.opensilk.video

import android.net.Uri
import org.opensilk.media.*
import org.opensilk.media.loader.cds.UPNP_ROOT_ID

/**
 * Created by drew on 8/6/17.
 */

const val TVDB_BANNER_ROOT = "http://tvdb.foo"

fun upnpDevices(): List<UpnpDeviceRef> {
    return listOf(
            UpnpDeviceRef(
                    UpnpDeviceId("foo0"),
                    UpnpDeviceMeta(
                            title = "Foo Server",
                            subtitle = "Mady by Foo",
                            artworkUri =  Uri.parse("http://foo.com/icon.jpg"),
                            updateId =  1002
                    )
            ),
            UpnpDeviceRef(
                    UpnpDeviceId("foo1"),
                    UpnpDeviceMeta(
                            title = "Foo Server"
                    )
            )
    )
}

fun upnpFolders(): List<UpnpFolderRef> {
    val list = ArrayList<UpnpFolderRef>()
    for (ii in 1..10) {
        list.add(UpnpFolderRef(
                UpnpFolderId("foo0", "$ii"),
                UpnpFolderId("foo0", UPNP_ROOT_ID),
                UpnpFolderMeta(
                        title = "Folder $ii"
                )
        ))
    }
    return list
}

fun upnpVideo_folder_1_no_association(): UpnpVideoRef {
    return UpnpVideoRef(
            UpnpVideoId("foo0", "1.1"),
            UpnpFolderId("foo0", "1"),
            null,
            null,
            UpnpVideoMeta(
                    mediaTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/1.01.mp4"),
                    mimeType = "video/mp4"
            )
    )
}

fun upnpVideo_folder_2_episode_id(): UpnpVideoRef {
    val ser = tvSeries()
    val ep = tvEpisode()
    return UpnpVideoRef(
            UpnpVideoId("foo0", "2.1"),
            UpnpFolderId("foo0", "2"),
            ep.id,
            null,
            UpnpVideoMeta(
                    title = ep.meta.title,
                    subtitle = "${ser.meta.title} - S0${ep.meta.seasonNumber}E0${ep.meta.episodeNumber}",
                    mediaTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/2.01.mp4"),
                    mimeType = "video/mp4",
                    artworkUri = Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(ep.meta.posterPath).build(),
                    backdropUri = Uri.parse(TVDB_BANNER_ROOT).buildUpon().appendPath(ep.meta.backdropPath).build()
            )
    )
}

fun upnpVideo_folder_3_movie_id(): UpnpVideoRef {
    return UpnpVideoRef(
            UpnpVideoId("foo0", "3.1"),
            UpnpFolderId("foo0", "3"),
            null,
            movie().id,
            UpnpVideoMeta(
                    title = movie().meta.title,
                    mediaTitle = "media.title.01",
                    mediaUri = Uri.parse("http://foo.com/media/3.01.mp4"),
                    mimeType = "video/mp4"
            )
    )
}

fun tvSeries(): TvSeriesRef {
    return TvSeriesRef(
            TvSeriesId(1),
            TvSeriesMeta(
                    title = "Series 1"
            )
    )
}

fun tvEpisode(): TvEpisodeRef {
    return TvEpisodeRef(
            TvEpisodeId(1, 1),
            TvEpisodeMeta(
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
            MovieId(1),
            MovieMeta(
                    title = "Movie 1",
                    overview = "Movie Overview"
            )
    )
}