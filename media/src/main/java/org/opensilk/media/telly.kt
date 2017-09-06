package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class TvSeriesId(val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvSeriesMeta(
        override val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val posterPath: String = "",
        val backdropPath: String = ""
): MediaMeta

data class TvSeriesRef(override val id: TvSeriesId, override val meta: TvSeriesMeta): MediaRef

data class TvEpisodeId(val episodeId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvEpisodeMeta(
        override val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val episodeNumber: Int,
        val seasonNumber: Int,
        val posterPath: String = "",
        val backdropPath: String = ""
): MediaMeta

data class TvEpisodeRef(override val id: TvEpisodeId, override val meta: TvEpisodeMeta): MediaRef

data class TvImageId(val imageId: Long, val seriesId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class TvImageMeta(
        override val title: String = "",
        val path: String,
        val type: String,
        val subType: String = "",
        val rating: Float = 0f,
        val ratingCount: Int = 0,
        val resolution: String = ""
): MediaMeta

data class TvImageRef(override val id: TvImageId, override val meta: TvImageMeta): MediaRef
