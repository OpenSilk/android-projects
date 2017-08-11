package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class TvSeriesId(val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvSeriesMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class TvSeriesRef(override val id: TvSeriesId, val meta: TvSeriesMeta): MediaRef

data class TvEpisodeId(val episodeId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvEpisodeMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val episodeNumber: Int,
        val seasonNumber: Int,
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class TvEpisodeRef(override val id: TvEpisodeId, val meta: TvEpisodeMeta): MediaRef

data class TvImageId(val imageId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvImageMeta(
        val path: String,
        val type: String,
        val subType: String = "",
        val rating: Float = 0f,
        val ratingCount: Int = 0,
        val resolution: String = ""
)

data class TvImageRef(override val id: TvImageId, val meta: TvImageMeta): MediaRef
