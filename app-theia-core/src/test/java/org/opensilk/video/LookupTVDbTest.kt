package org.opensilk.video

import android.net.Uri
import com.nhaarman.mockito_kotlin.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.opensilk.media.MediaMeta
import org.opensilk.media.MediaRef
import org.opensilk.media.UPNP_VIDEO
import org.opensilk.media.UpnpVideoId
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import rx.Observable
import rx.Single

/**
 * Created by drew on 7/26/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LookupTVDbTest {

    private val mAuth = Auth("foo")
    private val mToken = Token("baloney")
    private lateinit var mApi: TVDb
    private lateinit var mClient: DatabaseClient
    private lateinit var mLookup: LookupTVDb

    @Before
    fun setup() {
        mApi = mock()
        mClient = mock()
        mLookup = LookupTVDb(mAuth, mApi, mClient)
    }

    @Test
    fun token_no_cache_goes_to_network() {
        whenever(mClient.getTvToken())
                .thenReturn(Single.error(NoSuchItemException()))
        whenever(mApi.login(mAuth))
                .thenReturn(Observable.just(mToken))

        val tkn = mLookup.mTokenObservable.toBlocking().first()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).login(mAuth)
        verify(mClient).getTvToken()
        verify(mClient).setTvToken(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun token_from_cache_does_refresh() {
        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mApi.refreshToken(mToken))
                .thenReturn(Observable.just(mToken))

        val tkn = mLookup.mTokenObservable.toBlocking().first()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).refreshToken(mToken)
        verify(mClient).getTvToken()
        verify(mClient).setTvToken(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun token_is_subscribed_only_once() {
        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))

        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mApi.refreshToken(mToken))
                .thenReturn(Observable.just(mToken))

        val tkn = mLookup.mTokenObservable.toBlocking().first()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).refreshToken(mToken)
        verify(mClient).getTvToken()
        verify(mClient).setTvToken(mToken)

        val tkn2 = mLookup.mTokenObservable.toBlocking().first()
        assertThat(tkn2).isSameAs(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun lookup_no_association_goes_to_network() {
        val name = "Archer"
        val episodeName = "Mole Hunt"
        val seasonN = 1
        val episodeN = 1

        val meta = MediaMeta()
        meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "1")).toJson()
        meta.extras.putString(LOOKUP_NAME, name)
        meta.extras.putInt(LOOKUP_SEASON_NUM, seasonN)
        meta.extras.putInt(LOOKUP_EPISODE_NUM, episodeN)

        val seriesSearch = SeriesSearch(id = 1, seriesName = name)
        val seriesSearchData = SeriesSearchData(listOf(seriesSearch))
        val series = Series(id = 1, seriesName = name)
        val seriesData = SeriesData(series)
        val episode = SeriesEpisode(id = 1, airedSeason = seasonN,
                airedEpisodeNumber = episodeN, episodeName = episodeName)
        val episodeData = SeriesEpisodeData(listOf(episode))
        val imageData = SeriesImageQueryData(emptyList())
        val seriesUri = Uri.parse("foo.com/1")
        val seriesMeta = MediaMeta()
        seriesMeta.title = episodeName
        seriesMeta.seasonNumber = seasonN
        seriesMeta.episodeNumber = episodeN

        whenever(mApi.refreshToken(mToken))
                .thenReturn(Observable.just(mToken))
        whenever(mApi.searchSeries(mToken, name))
                .thenReturn(Observable.just(seriesSearchData))
        whenever(mApi.series(mToken, 1))
                .thenReturn(Observable.just(seriesData))
        whenever(mApi.seriesEpisodes(mToken, 1))
                .thenReturn(Observable.just(episodeData))
        whenever(mApi.seriesImagesQuery(eq(mToken), eq(1),  Mockito.anyString()))
                .thenReturn(Observable.just(imageData))

        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mClient.getTvSeriesAssociation(name))
                .thenReturn(Single.error(LookupException()))
        whenever(mClient.addTvSeries(series))
                .thenReturn(seriesUri)
        whenever(mClient.getTvEpisodes(1))
                .thenReturn(Observable.just(seriesMeta))

        val list = mLookup.lookupObservable(meta).toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(seriesMeta)

        verify(mApi).refreshToken(mToken)
        verify(mApi).searchSeries(mToken, name)
        verify(mApi).series(mToken, 1)
        verify(mApi).seriesEpisodes(mToken, 1)
        verify(mApi, times(2)).seriesImagesQuery(eq(mToken), eq(1), Mockito.anyString())

        verify(mClient).getTvToken()
        verify(mClient).getTvSeriesAssociation(name)
        verify(mClient).addTvSeries(series)
        verify(mClient).getTvEpisodes(1)

        //non mocked interactions
        verify(mClient).setTvToken(mToken)
        verify(mClient, times(2)).addTvImages(1, emptyList())
        verify(mClient).addTvEpisodes(1, listOf(episode))

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun lookup_with_association_does_not_touch_network() {
        val name = "Archer"
        val episodeName = "Mole Hunt"
        val seasonN = 1
        val episodeN = 1

        val meta = MediaMeta()
        meta.mediaId = MediaRef(UPNP_VIDEO, UpnpVideoId("foo", "1")).toJson()
        meta.extras.putString(LOOKUP_NAME, name)
        meta.extras.putInt(LOOKUP_SEASON_NUM, seasonN)
        meta.extras.putInt(LOOKUP_EPISODE_NUM, episodeN)

        val seriesUri = Uri.parse("foo.com/1")
        val seriesMeta = MediaMeta()
        seriesMeta.title = episodeName
        seriesMeta.seasonNumber = seasonN
        seriesMeta.episodeNumber = episodeN

        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mClient.getTvSeriesAssociation(name))
                .thenReturn(Single.just(1))
        whenever(mClient.getTvEpisodes(1))
                .thenReturn(Observable.just(seriesMeta))

        val list = mLookup.lookupObservable(meta).toList().toBlocking().first()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(seriesMeta)

        verify(mClient).getTvToken()
        verify(mClient).getTvSeriesAssociation(name)
        verify(mClient).getTvEpisodes(1)

        verifyZeroInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }
}