package org.opensilk.tvdb.api

import org.opensilk.tvdb.api.model.*
import retrofit2.http.*
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by drew on 7/25/17.
 */
interface TVDb {

    @POST("/login")
    fun login(@Body auth: Auth): Single<Token>

    @GET("/refresh_token")
    fun refreshToken(@Header("Authorization") token: Token): Single<Token>

    @GET("/search/series")
    fun searchSeries(@Header("Authorization") token: Token, @Query("name") name: String) : Single<SeriesSearchData>

    @GET("/series/{id}")
    fun series(@Header("Authorization") token: Token, @Path("id") id: Long): Single<SeriesData>

    @GET("/series/{id}/episodes")
    fun seriesEpisodes(@Header("Authorization") token: Token, @Path("id") id: Long,
                       @Query("page") page: Int = 1): Single<SeriesEpisodeData>

    @GET("/series/{id}/images/query")
    fun seriesImagesQuery(@Header("Authorization") token: Token, @Path("id") id: Long,
                          @Query("keyType") keyType: String): Single<SeriesImageQueryData>

    @GET("/updated/query")
    fun updated(@Header("Authorization") token: Token, @Query("fromTime") fromTime: Long): Single<UpdateData>

    @GET("/updated/query")
    fun updated(@Header("Authorization") token: Token, @Query("fromTime") fromTime: Long,
                @Query("toTime") toTime: Long): Single<UpdateData>
}