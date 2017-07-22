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
    val TV_SERIES_ONE_EPISODES = 3
    val TV_EPISODES = 4
    val TV_EPISODES_ONE = 5
    val TV_BANNERS = 6
    val TV_BANNERS_ONE = 7
    val TV_ACTORS = 8
    val TV_ACTORS_ONE = 9
    val TV_LOOKUPS = 10
    //
    //
    val TV_EPISODE_DESC = 12
    val TV_EPISODE_DESC_ONE = 13
    val TV_SERIES_SEARCH = 14

    val MEDIA = 20
    val MEDIA_ONE = 21

    val MOVIES = 30
    val MOVIES_ONE = 31
    val MOVIE_IMAGES = 32
    val MOVIE_IMAGES_ONE = 33
    val MOVIE_LOOKUPS = 34
    val MOVIE_SEARCH = 35

    val UPNP_DEVICES = 41
    val UPNP_DEVICES_ONE = 42
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
        matcher.addURI(mAuthority, "tv/series/#/episodes", DatabaseMatches.TV_SERIES_ONE_EPISODES)
        matcher.addURI(mAuthority, "tv/series/search", DatabaseMatches.TV_SERIES_SEARCH)
        matcher.addURI(mAuthority, "tv/episodes", DatabaseMatches.TV_EPISODES)
        matcher.addURI(mAuthority, "tv/episodes/#", DatabaseMatches.TV_EPISODES_ONE)
        matcher.addURI(mAuthority, "tv/banners", DatabaseMatches.TV_BANNERS)
        matcher.addURI(mAuthority, "tv/banners/#", DatabaseMatches.TV_BANNERS_ONE)
        matcher.addURI(mAuthority, "tv/actors", DatabaseMatches.TV_ACTORS)
        matcher.addURI(mAuthority, "tv/actors/#", DatabaseMatches.TV_ACTORS_ONE)
        matcher.addURI(mAuthority, "tv/lookups", DatabaseMatches.TV_LOOKUPS)
        matcher.addURI(mAuthority, "tv/episodes/descriptions", DatabaseMatches.TV_EPISODE_DESC)
        matcher.addURI(mAuthority, "tv/episodes/descriptions/#", DatabaseMatches.TV_EPISODE_DESC_ONE)

        matcher.addURI(mAuthority, "media", DatabaseMatches.MEDIA)
        matcher.addURI(mAuthority, "media/#", DatabaseMatches.MEDIA_ONE)

        matcher.addURI(mAuthority, "movies", DatabaseMatches.MOVIES)
        matcher.addURI(mAuthority, "movies/#", DatabaseMatches.MOVIES_ONE)
        matcher.addURI(mAuthority, "movies/images", DatabaseMatches.MOVIE_IMAGES)
        matcher.addURI(mAuthority, "movies/images/#", DatabaseMatches.MOVIE_IMAGES_ONE)
        matcher.addURI(mAuthority, "movies/lookups", DatabaseMatches.MOVIE_LOOKUPS)
        matcher.addURI(mAuthority, "movies/search", DatabaseMatches.MOVIE_SEARCH)

        matcher.addURI(mAuthority, "upnp/device", DatabaseMatches.UPNP_DEVICES)
        matcher.addURI(mAuthority, "upnp/device/#", DatabaseMatches.UPNP_DEVICES_ONE)
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

    fun tvSeriesEpisodes(id: Long): Uri {
        return ContentUris.appendId(tvSeries().buildUpon(), id).appendPath("episodes").build()
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

    fun tvActors(): Uri {
        return base().appendPath("tv").appendPath("actors").build()
    }

    fun tvActor(id: Long): Uri {
        return ContentUris.withAppendedId(tvActors(), id)
    }

    fun tvLookups(): Uri {
        return base().appendPath("tv").appendPath("lookups").build()
    }

    fun tvEpisodeDescriptions(): Uri {
        return base().appendPath("tv").appendPath("episodes").appendPath("descriptions").build()
    }

    fun tvEpisodeDescription(id: Long): Uri {
        return ContentUris.withAppendedId(tvEpisodeDescriptions(), id)
    }

    fun media(): Uri {
        return base().appendPath("media").build()
    }

    fun media(id: Long): Uri {
        return ContentUris.withAppendedId(media(), id)
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

    fun movieLookups(): Uri {
        return base().appendPath("movies").appendPath("lookups").build()
    }

    fun movieSearch(): Uri {
        return base().appendPath("movies").appendPath("search").build()
    }

    fun upnpDevices(): Uri {
        return base().appendPath("upnp").appendPath("device").build()
    }

    fun upnpDevice(id: Long): Uri {
        return ContentUris.withAppendedId(upnpDevices(), id)
    }

}