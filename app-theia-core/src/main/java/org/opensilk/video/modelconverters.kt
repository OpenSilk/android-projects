package org.opensilk.video

import android.net.Uri
import org.fourthline.cling.model.meta.Device
import org.fourthline.cling.model.meta.RemoteDeviceIdentity
import org.opensilk.media.*
import org.opensilk.tmdb.api.model.Image
import org.opensilk.tmdb.api.model.Movie
import org.opensilk.tvdb.api.model.Series
import org.opensilk.tvdb.api.model.SeriesEpisode
import org.opensilk.tvdb.api.model.SeriesImageQuery

/**
 * Created by drew on 8/6/17.
 */
fun Device<*, *, *>.toMediaMeta(): UpnpDeviceRef {
    var mediaId = UpnpDeviceId(identity.udn.identifierString)
    var title = details.friendlyName ?: displayString
    var subtitle = if (displayString == title) "" else displayString
    var artwork = Uri.EMPTY
    if (hasIcons()) {
        var largest = icons.asSequence().fold(icons[0], { l, r -> if (l.height < r.height) r else l })
        var uri = largest.uri.toString()
        //TODO fragile... only tested on MiniDLNA
        if (uri.startsWith("/")) {
            val ident = identity
            if (ident is RemoteDeviceIdentity) {
                val ru = ident.descriptorURL
                uri = "${ru.protocol}://${ru.host}:${ru.port}${uri}"
            }
        }
        artwork = Uri.parse(uri)
    }
    return UpnpDeviceRef(
            mediaId,
            UpnpDeviceMeta(
                    title = title,
                    subtitle = subtitle,
                    artworkUri = artwork
            )
    )
}

fun Series.toTvSeriesRef(poster: SeriesImageQuery?, backdrop: SeriesImageQuery?): TvSeriesRef {
    return TvSeriesRef(
            TvSeriesId(id),
            TvSeriesMeta(
                    title = seriesName,
                    overview = overview,
                    releaseDate = firstAired,
                    posterPath = poster?.fileName ?: "",
                    backdropPath = backdrop?.fileName ?: ""
            )
    )
}

fun SeriesEpisode.toTvEpisodeRef(seriesId: Long, poster: SeriesImageQuery?, backdrop: SeriesImageQuery?): TvEpisodeRef {
    return TvEpisodeRef(
            TvEpisodeId(id, seriesId),
            TvEpisodeMeta(
                    title = episodeName,
                    overview = overview,
                    releaseDate = firstAired,
                    episodeNumber = airedEpisodeNumber,
                    seasonNumber = airedSeason,
                    posterPath = poster?.fileName ?: "",
                    backdropPath = backdrop?.fileName ?: ""
            )
    )
}

fun SeriesImageQuery.toTvImage(seriesId: Long): TvImageRef {
    return TvImageRef(
            TvImageId(id, seriesId),
            TvImageMeta(
                    path = fileName,
                    type = keyType,
                    subType = subKey,
                    rating = ratingsInfo.average,
                    ratingCount = ratingsInfo.count,
                    resolution = resolution
            )
    )
}

fun Movie.toMovieRef(): MovieRef {
    return MovieRef(
            MovieId(id),
            MovieMeta(
                    title = title,
                    overview = overview ?: "",
                    releaseDate = releaseDate ?: "",
                    posterPath = posterPath ?: "",
                    backdropPath = backdropPath ?: ""
            )
    )
}

fun Image.toMovieImageRef(movieId: Long, type: String): MovieImageRef {
    return MovieImageRef(
            MovieImageId(-1, movieId),
            MovieImageMeta(
                    path = filePath,
                    type = type,
                    resolution = "${width}x$height",
                    rating = voteAverage ?: 0f,
                    ratingCount = voteCount ?: 0
            )
    )
}
