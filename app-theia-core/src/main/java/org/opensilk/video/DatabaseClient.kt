package org.opensilk.video

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import dagger.Binds
import dagger.Module
import org.apache.commons.lang3.StringUtils
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media.MediaRef
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.playback.MediaProviderClient
import org.opensilk.tmdb.api.model.Image
import org.opensilk.tmdb.api.model.ImageList
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tmdb.api.model.TMDbConfig
import org.opensilk.tvdb.api.model.*
import rx.Observable
import rx.Single
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class MediaProviderModule {
    @Binds abstract fun providerClient(providerClient: DatabaseClient): MediaProviderClient
}

/**
 *
 */
class NoSuchItemException: Exception()

/**
 * Created by drew on 7/18/17.
 */
@Singleton
class DatabaseClient
@Inject constructor(
    @ForApplication context: Context,
    private val mUris: DatabaseUris,
    @Named("TVDBRoot") tvdbRootUri: Uri
) : MediaProviderClient {

    private val mResolver = context.contentResolver
    val tvdb = TVDbClient(tvdbRootUri)
    val tmdb = MovieDbClient()

    override fun getMediaItem(mediaRef: MediaRef): Single<MediaBrowser.MediaItem> {
        TODO("not implemented")
    }

    fun getVideoDescription(mediaRef: MediaRef): Single<VideoDescInfo> {
        return Single.create { s ->
            s.onError(NoSuchItemException())
        }
    }


    inner class TVDbClient(private val tvdbRoot: Uri) {

        fun rootUri(): Uri {
            return tvdbRoot
        }

        fun bannerRootUri(): Uri {
            return Uri.withAppendedPath(tvdbRoot, "banners/")
        }

        fun makeBannerUri(path: String): Uri {
            return Uri.withAppendedPath(bannerRootUri(), path)
        }

        fun makeSubtitle(seriesName: String, seasonNumber: Int, episodeNumber: Int): String {
            return String.format(Locale.getDefault(), "%s - S%02dE%02d", seriesName, seasonNumber, episodeNumber)
        }

        fun insertAllZipped(allZipped: AllZipped) {
            insertTvSeries(allZipped.series)
            if (allZipped.episodes != null && !allZipped.episodes.isEmpty()) {
                for (episode in allZipped.episodes) {
                    insertEpisode(episode)
                }
            }
            if (allZipped.banners != null && !allZipped.banners.isEmpty()) {
                for (banner in allZipped.banners) {
                    insertBanner(allZipped.series.id!!, banner)
                }
            }
            if (allZipped.actors != null && !allZipped.actors.isEmpty()) {
                for (actor in allZipped.actors) {
                    insertActor(allZipped.series.id!!, actor)
                }
            }
        }

        fun insertTvSeries(series: Series): Uri {
            val values = ContentValues(10)
            values.put("_id", series.id)
            values.put("_display_name", series.seriesName)
            if (!series.overview.isNullOrEmpty()) {
                values.put("overview", series.overview)
            }
            if (!series.firstAired.isNullOrEmpty()) {
                values.put("first_aired", series.firstAired)
            }
            if (!series.posterPath.isNullOrEmpty()) {
                values.put("poster_path", series.posterPath)
            }
            if (!series.fanartPath.isNullOrEmpty()) {
                values.put("backdrop_path", series.fanartPath)
            }
            return mResolver.insert(mUris.tvSeries(), values)
        }

        fun insertEpisode(episode: Episode): Uri {
            val values = ContentValues(10)
            values.put("_id", episode.id)
            if (!episode.episodeName.isNullOrEmpty()) {
                values.put("_display_name", episode.episodeName)
            }
            if (!episode.overview.isNullOrEmpty()) {
                values.put("overview", episode.overview)
            }
            if (!episode.firstAired.isNullOrEmpty()) {
                values.put("first_aired", episode.firstAired)
            }
            values.put("episode_number", episode.episodeNumber)
            values.put("season_number", episode.seasonNumber)
            values.put("series_id", episode.seriesId)
            return mResolver.insert(mUris.tvEpisodes(), values)
        }

        fun insertBanner(seriesId: Long, banner: Banner): Uri {
            val values = ContentValues(10)
            values.put("_id", banner.id)
            values.put("path", banner.bannerPath)
            values.put("type", banner.bannerType)
            values.put("type2", banner.bannerType2)
            if (banner.rating != null) {
                values.put("rating", banner.rating)
            }
            if (banner.ratingCount != null) {
                values.put("rating_count", banner.ratingCount)
            }
            if (banner.thumbnailPath != null) {
                values.put("thumb_path", banner.thumbnailPath)
            }
            if (banner.season != null) {
                values.put("season", banner.season)
            }
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvBanners(), values)
        }

        fun insertActor(seriesId: Long, actor: Actor): Uri {
            val values = ContentValues(10)
            values.put("_id", actor.id)
            values.put("_display_name", actor.name)
            values.put("role", actor.role)
            values.put("sort_order", actor.sortOrder)
            if (!actor.imagePath.isNullOrEmpty()) {
                values.put("image_path", actor.imagePath)
            }
            values.put("series_id", seriesId)
            return mResolver.insert(mUris.tvActors(), values)
        }

    }

    inner class MovieDbClient {

        fun makePosterUri(base: String, path: String): Uri {
            return Uri.parse("${base}w342$path")
        }

        fun makeBackdropUri(base: String, path: String): Uri {
            return Uri.parse("${base}w1280$path")
        }

        fun updateConfig(config: TMDbConfig) {
            val values = ContentValues()
            values.put("image_base_url", config.images.baseUrl)
            try {
                mResolver.update(mUris.movies(), values, null, null)
                mResolver.update(mUris.movieImages(), values, null, null)
            } catch (e: SQLiteException) {
                Timber.e(e, "updateConfig %s", config)
            }
        }

        fun setMovieAssociation(q: String, id: Long) {
            val contentValues = ContentValues(2)
            contentValues.put("q", q)
            contentValues.put("movie_id", id)
            mResolver.insert(mUris.movieLookups(), contentValues)
        }

        fun insertMovie(movie: Movie, config: TMDbConfig): Uri {
            val values = ContentValues(10)
            values.put("_id", movie.id)
            values.put("_display_name", movie.title)
            values.put("overview", movie.overview)
            values.put("release_date", movie.releaseDate)
            values.put("poster_path", movie.posterPath)
            values.put("backdrop_path", movie.backdropPath)
            values.put("image_base_url", config.images.secureBaseUrl)
            return mResolver.insert(mUris.movies(), values)
        }

        fun insertImages(imageList: ImageList, config: TMDbConfig) {
            val numPosters = if (imageList.posters != null) imageList.posters.size else 0
            val numBackdrops = if (imageList.posters != null) imageList.backdrops.size else 0
            val contentValues = arrayOfNulls<ContentValues>(numPosters + numBackdrops)
            var idx = 0
            if (numPosters > 0) {
                for (image in imageList.posters) {
                    val values = makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_base_url", config.images.secureBaseUrl)
                    values.put("image_type", "poster")
                    contentValues[idx++] = values
                }
            }
            if (numBackdrops > 0) {
                for (image in imageList.backdrops) {
                    val values = makeImageValues(image)
                    values.put("movie_id", imageList.id)
                    values.put("image_base_url", config.images.secureBaseUrl)
                    values.put("image_type", "backdrop")
                    contentValues[idx++] = values
                }
            }
            mResolver.bulkInsert(mUris.movieImages(), contentValues)
        }

        internal fun makeImageValues(image: Image): ContentValues {
            val values = ContentValues(10)
            values.put("height", image.height)
            values.put("width", image.width)
            values.put("file_path", image.filePath)
            values.put("vote_average", image.voteAverage)
            values.put("vote_count", image.voteCount)
            return values
        }

    }

}
