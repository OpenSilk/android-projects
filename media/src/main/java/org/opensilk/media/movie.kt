package org.opensilk.media

/**
 * Created by drew on 8/11/17.
 */
data class MovieId(val movieId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class MovieMeta(
        val title: String,
        val overview: String = "",
        val releaseDate: String = "",
        val posterPath: String = "",
        val backdropPath: String = ""
)

data class MovieRef(override val id: MovieId, val meta: MovieMeta): MediaRef

data class MovieImageId(val imageId: Long, val movieId: Long): MediaId {
    override val json: String
        get() = TODO("not implemented")
}

data class MovieImageMeta(
        val path: String,
        val type: String,
        val rating: Float = 0f,
        val ratingCount: Int = 0,
        val resolution: String = ""
)

data class MovieImageRef(override val id: MovieImageId, val meta: MovieImageMeta): MediaRef
