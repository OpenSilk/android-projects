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
    val UPNP_MUSIC_TRACK = 204
    val UPNP_VIDEO = 205

    val DOCUMENT_DIRECTORY = 301
    val DOCUMENT_VIDEO = 302
    val DOCUMENT_AUDIO = 303

    val PLAYBACK_POSITION = 401

    val STORAGE_DEVICE = 501
    val STORAGE_FOLDER = 502
    val STORAGE_VIDEO = 503
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
        matcher.addURI(mAuthority, "movie/image", M.MOVIE_IMAGE)

        matcher.addURI(mAuthority, "upnp/audio", M.UPNP_AUDIO)
        matcher.addURI(mAuthority, "upnp/device", M.UPNP_DEVICE)
        matcher.addURI(mAuthority, "upnp/folder", M.UPNP_FOLDER)
        matcher.addURI(mAuthority, "upnp/music/track", M.UPNP_MUSIC_TRACK)
        matcher.addURI(mAuthority, "upnp/video", M.UPNP_VIDEO)

        matcher.addURI(mAuthority, "document/directory", M.DOCUMENT_DIRECTORY)
        matcher.addURI(mAuthority, "document/video", M.DOCUMENT_VIDEO)
        matcher.addURI(mAuthority, "document/audio", M.DOCUMENT_AUDIO)

        matcher.addURI(mAuthority, "playback/position", M.PLAYBACK_POSITION)

        matcher.addURI(mAuthority, "storage/device", M.STORAGE_DEVICE)
        matcher.addURI(mAuthority, "storage/folder", M.STORAGE_FOLDER)
        matcher.addURI(mAuthority, "storage/video", M.STORAGE_VIDEO)
    }

    private fun base() = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(mAuthority)

    fun tvSeries() = base().appendPath("tv").appendPath("series").build()

    fun tvEpisode() = base().appendPath("tv").appendPath("episode").build()

    fun tvImage() = base().appendPath("tv").appendPath("image").build()

    fun movie() = base().appendPath("movie").build()

    fun movieImage() = base().appendPath("movie").appendPath("image").build()

    fun upnpAudio() = base().appendPath("upnp").appendPath("audio").build()

    fun upnpDevice() = base().appendPath("upnp").appendPath("device").build()

    fun upnpFolder() = base().appendPath("upnp").appendPath("folder").build()

    fun upnpMusicTrack() = base().appendPath("upnp").appendPath("music").appendPath("track").build()

    fun upnpVideo() = base().appendPath("upnp").appendPath("video").build()

    fun playbackPosition() = base().appendPath("playback").appendPath("position").build()

    fun documentDirectory() = base().appendPath("document").appendPath("directory").build()

    fun documentVideo() = base().appendPath("document").appendPath("video").build()

    fun documentAudio() = base().appendPath("document").appendPath("audio").build()

    fun storageDevice() = base().appendPath("storage").appendPath("device").build()

    fun storageFolder() = base().appendPath("storage").appendPath("folder").build()

    fun storageVideo() = base().appendPath("storage").appendPath("video").build()

}