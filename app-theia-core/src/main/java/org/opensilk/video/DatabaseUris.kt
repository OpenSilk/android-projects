package org.opensilk.video

import android.content.ContentResolver
import android.content.ContentUris
import android.content.UriMatcher
import android.net.Uri
import javax.inject.Inject
import javax.inject.Named

object DatabaseMatches {
    val TV_SERIES = 1
    val TV_SERIES_ONE = 2
    val TV_SERIES_SEARCH = 3
    val TV_EPISODES = 4
    val TV_EPISODES_ONE = 5
    val TV_BANNERS = 6
    val TV_BANNERS_ONE = 7
    val TV_CONFIG = 11

    val MOVIES = 30
    val MOVIES_ONE = 31
    val MOVIE_IMAGES = 32
    val MOVIE_IMAGES_ONE = 33
    val MOVIE_SEARCH = 35
    val MOVIE_CONFIG = 36

    val UPNP_DEVICES = 41
    val UPNP_DEVICES_ONE = 42
    val UPNP_DEVICES_SCAN_UP = 43
    val UPNP_DEVICES_SCAN_DOWN = 44

    val UPNP_FOLDERS = 51
    val UPNP_FOLDERS_ONE = 52

    val UPNP_VIDEOS = 61
    val UPNP_VIDEOS_ONE = 62

    val PLAYBACK_POSITION = 71

    val DOCUMENTS = 81
    val DOCUMENTS_ONE = 82
}

/**
 * Created by drew on 7/18/17.
 */
class DatabaseUris
@Inject constructor(
        @Named("DatabaseAuthority") private val mAuthority: String
) {

    val matcher = UriMatcher(UriMatcher.NO_MATCH)
    init {
        matcher.addURI(mAuthority, "tv/series", DatabaseMatches.TV_SERIES)
        matcher.addURI(mAuthority, "tv/series/#", DatabaseMatches.TV_SERIES_ONE)
        matcher.addURI(mAuthority, "tv/series/search", DatabaseMatches.TV_SERIES_SEARCH)
        matcher.addURI(mAuthority, "tv/episodes", DatabaseMatches.TV_EPISODES)
        matcher.addURI(mAuthority, "tv/episodes/#", DatabaseMatches.TV_EPISODES_ONE)
        matcher.addURI(mAuthority, "tv/banners", DatabaseMatches.TV_BANNERS)
        matcher.addURI(mAuthority, "tv/banners/#", DatabaseMatches.TV_BANNERS_ONE)
        matcher.addURI(mAuthority, "tv/config", DatabaseMatches.TV_CONFIG)

        matcher.addURI(mAuthority, "movies", DatabaseMatches.MOVIES)
        matcher.addURI(mAuthority, "movies/#", DatabaseMatches.MOVIES_ONE)
        matcher.addURI(mAuthority, "movies/images", DatabaseMatches.MOVIE_IMAGES)
        matcher.addURI(mAuthority, "movies/images/#", DatabaseMatches.MOVIE_IMAGES_ONE)
        matcher.addURI(mAuthority, "movies/search", DatabaseMatches.MOVIE_SEARCH)
        matcher.addURI(mAuthority, "movies/config", DatabaseMatches.MOVIE_CONFIG)

        matcher.addURI(mAuthority, "upnp/device", DatabaseMatches.UPNP_DEVICES)
        matcher.addURI(mAuthority, "upnp/device/#", DatabaseMatches.UPNP_DEVICES_ONE)
        matcher.addURI(mAuthority, "upnp/device/scan/up", DatabaseMatches.UPNP_DEVICES_SCAN_UP)
        matcher.addURI(mAuthority, "upnp/device/scan/down", DatabaseMatches.UPNP_DEVICES_SCAN_DOWN)

        matcher.addURI(mAuthority, "upnp/folder", DatabaseMatches.UPNP_FOLDERS)
        matcher.addURI(mAuthority, "upnp/folder/#", DatabaseMatches.UPNP_FOLDERS_ONE)

        matcher.addURI(mAuthority, "upnp/video", DatabaseMatches.UPNP_VIDEOS)
        matcher.addURI(mAuthority, "upnp/video/#", DatabaseMatches.UPNP_VIDEOS_ONE)

        matcher.addURI(mAuthority, "playback/position", DatabaseMatches.PLAYBACK_POSITION)

        matcher.addURI(mAuthority, "documents", DatabaseMatches.DOCUMENTS)
        matcher.addURI(mAuthority, "documents/#", DatabaseMatches.DOCUMENTS_ONE)
    }

    private fun base(): Uri.Builder {
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(mAuthority)
    }

    fun tvSeries(): Uri {
        return base().appendPath("tv").appendPath("series").build()
    }

    fun tvSeries(id: Long): Uri {
        return ContentUris.withAppendedId(tvSeries(), id)
    }

    fun tvSeriesSearch(): Uri {
        return base().appendPath("tv").appendPath("series").appendPath("search").build()
    }

    fun tvEpisodes(): Uri {
        return base().appendPath("tv").appendPath("episodes").build()
    }

    fun tvEpisode(id: Long): Uri {
        return ContentUris.withAppendedId(tvEpisodes(), id)
    }

    fun tvBanners(): Uri {
        return base().appendPath("tv").appendPath("banners").build()
    }

    fun tvBanner(id: Long): Uri {
        return ContentUris.withAppendedId(tvBanners(), id)
    }

    fun tvConfig(): Uri {
        return base().appendPath("tv").appendPath("config").build()
    }

    fun movies(): Uri {
        return base().appendPath("movies").build()
    }

    fun movie(id: Long): Uri {
        return ContentUris.withAppendedId(movies(), id)
    }

    fun movieImages(): Uri {
        return base().appendPath("movies").appendPath("images").build()
    }

    fun movieImage(id: Long): Uri {
        return ContentUris.withAppendedId(movieImages(), id)
    }

    fun movieSearch(): Uri {
        return base().appendPath("movies").appendPath("search").build()
    }

    fun movieConfig(): Uri {
        return base().appendPath("movies").appendPath("config").build()
    }

    fun upnpDevices(): Uri {
        return base().appendPath("upnp").appendPath("device").build()
    }

    fun upnpDevice(id: Long): Uri {
        return ContentUris.withAppendedId(upnpDevices(), id)
    }

    fun upnpDeviceIncrementScanning(): Uri {
        return base().appendPath("upnp").appendPath("device").appendPath("scan").appendPath("up").build()
    }

    fun upnpDeviceDecrementScanning(): Uri {
        return base().appendPath("upnp").appendPath("device").appendPath("scan").appendPath("down").build()
    }

    fun upnpFolders(): Uri {
        return base().appendPath("upnp").appendPath("folder").build()
    }

    fun upnpFolder(id: Long): Uri {
        return ContentUris.withAppendedId(upnpFolders(), id)
    }

    fun upnpVideos(): Uri {
        return base().appendPath("upnp").appendPath("video").build()
    }

    fun upnpVideo(id: Long): Uri {
        return ContentUris.withAppendedId(upnpVideos(), id)
    }

    fun playbackPosition(): Uri {
        return base().appendPath("playback").appendPath("position").build()
    }

    fun documents(): Uri {
        return base().appendPath("documents").build()
    }

    fun document(id: Long): Uri {
        return ContentUris.withAppendedId(documents(), id)
    }

}