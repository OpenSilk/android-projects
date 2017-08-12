package org.opensilk.media.database

import android.content.ContentResolver
import android.content.UriMatcher
import android.net.Uri
import javax.inject.Inject
import javax.inject.Named

val URI_SUCCESS = Uri.parse("/success")
val URI_FAILURE = Uri.parse("/failure")

internal object M {
    val TV_SERIES = 1
    val TV_EPISODE = 2
    val TV_IMAGE = 3

    val MOVIE = 101
    val MOVIE_IMAGE = 102

    val UPNP_AUDIO = 201
    val UPNP_DEVICE = 202
    val UPNP_FOLDER = 203
    val UPNP_MUSIC_ALBUM = 204
    val UPNP_MUSIC_ARTIST = 205
    val UPNP_MUSIC_GENRE = 206
    val UPNP_MUSIC_TRACK = 207
    val UPNP_VIDEO = 208

    val DOCUMENT = 301

    val PLAYBACK_POSITION = 401
}

/**
 * Created by drew on 7/18/17.
 */
class MediaDBUris
@Inject constructor(
        @Named("MediaDatabaseAuthority") private val mAuthority: String
) {

    val matcher = UriMatcher(UriMatcher.NO_MATCH)
    init {
        matcher.addURI(mAuthority, "tv/series", M.TV_SERIES)
        matcher.addURI(mAuthority, "tv/episode", M.TV_EPISODE)
        matcher.addURI(mAuthority, "tv/image", M.TV_IMAGE)

        matcher.addURI(mAuthority, "movie", M.MOVIE)
        matcher.addURI(mAuthority, "movies/image", M.MOVIE_IMAGE)

        matcher.addURI(mAuthority, "upnp/audio", M.UPNP_AUDIO)
        matcher.addURI(mAuthority, "upnp/device", M.UPNP_DEVICE)
        matcher.addURI(mAuthority, "upnp/folder", M.UPNP_FOLDER)
        matcher.addURI(mAuthority, "upnp/music/album", M.UPNP_MUSIC_ALBUM)
        matcher.addURI(mAuthority, "upnp/music/artist", M.UPNP_MUSIC_ARTIST)
        matcher.addURI(mAuthority, "upnp/music/genre", M.UPNP_MUSIC_GENRE)
        matcher.addURI(mAuthority, "upnp/music/track", M.UPNP_MUSIC_TRACK)
        matcher.addURI(mAuthority, "upnp/video", M.UPNP_VIDEO)

        matcher.addURI(mAuthority, "document", M.DOCUMENT)

        matcher.addURI(mAuthority, "playback/position", M.PLAYBACK_POSITION)
    }

    private fun base(): Uri.Builder {
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(mAuthority)
    }

    fun tvSeries(): Uri {
        return base().appendPath("tv").appendPath("series").build()
    }

    fun tvEpisode(): Uri {
        return base().appendPath("tv").appendPath("episode").build()
    }

    fun tvImage(): Uri {
        return base().appendPath("tv").appendPath("image").build()
    }

    fun movie(): Uri {
        return base().appendPath("movies").build()
    }

    fun movieImage(): Uri {
        return base().appendPath("movies").appendPath("image").build()
    }

    fun upnpAudio(): Uri {
        return base().appendPath("upnp").appendPath("audio").build()
    }

    fun upnpDevice(): Uri {
        return base().appendPath("upnp").appendPath("device").build()
    }

    fun upnpFolder(): Uri {
        return base().appendPath("upnp").appendPath("folder").build()
    }

    fun upnpMusicAlbum(): Uri {
        return base().appendPath("upnp").appendPath("music").appendPath("album").build()
    }

    fun upnpMusicArtist(): Uri {
        return base().appendPath("upnp").appendPath("music").appendPath("artist").build()
    }

    fun upnpMusicGenre(): Uri {
        return base().appendPath("upnp").appendPath("music").appendPath("genre").build()
    }

    fun upnpMusicTrack(): Uri {
        return base().appendPath("upnp").appendPath("music").appendPath("track").build()
    }

    fun upnpVideo(): Uri {
        return base().appendPath("upnp").appendPath("video").build()
    }

    fun playbackPosition(): Uri {
        return base().appendPath("playback").appendPath("position").build()
    }

    fun document(): Uri {
        return base().appendPath("document").build()
    }

}