package org.opensilk.autumn

import io.reactivex.Single
import retrofit2.http.GET

/**
 * Created by drew on 8/4/17.
 */
interface NetworkApi {
    @GET("entries.json")
    fun getPlaylists(): Single<List<Playlist>>
}