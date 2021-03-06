package org.opensilk.video

import com.nhaarman.mockito_kotlin.*
import io.reactivex.Observable
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.opensilk.media.TvEpisodeRef
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.testdata.tvEpisode
import org.opensilk.media.testdata.tvSeries
import org.opensilk.media.testdata.upnpVideo_folder_1_no_association
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Created by drew on 7/26/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class LookupTVDbTest {

    private val mAuth = Auth("foo")
    private val mToken = Token("baloney")
    private lateinit var mApi: TVDb
    private lateinit var mClient: MediaDAO
    private lateinit var mVideoClient: VideoAppDAO
    private lateinit var mLookup: LookupTVDb

    @Before
    fun setup() {
        mApi = mock()
        mClient = mock()
        mVideoClient = mock()
        mLookup = LookupTVDb(mAuth, mApi, mClient, mVideoClient)
    }

    @Test
    fun token_no_cache_goes_to_network() {
        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.error(Exception()))
        whenever(mApi.login(mAuth))
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).login(mAuth)
        verify(mVideoClient).getTvToken()
        verify(mVideoClient).setTvToken(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun token_from_cache_does_refresh() {
        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mApi.refreshToken(mToken))
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).refreshToken(mToken)
        verify(mVideoClient).getTvToken()
        verify(mVideoClient).setTvToken(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun token_is_subscribed_only_once() {
        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.just(mToken))

        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mApi.refreshToken(mToken))
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).refreshToken(mToken)
        verify(mVideoClient).getTvToken()
        verify(mVideoClient).setTvToken(mToken)

        val tkn2 = mLookup.mTokenObservable.blockingFirst()
        assertThat(tkn2).isSameAs(mToken)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun lookup_no_association_goes_to_network() {
        val name = "Archer"
        val episodeName = "Mole Hunt"
        val seasonN = 1
        val episodeN = 1

        val meta = LookupRequest(upnpVideo_folder_1_no_association())
        meta.lookupName = name
        meta.seasonNumber = seasonN
        meta.episodeNumber = episodeN

        val seriesSearch = SeriesSearch(id = 1, seriesName = name)
        val seriesSearchData = SeriesSearchData(listOf(seriesSearch))
        val series = Series(id = 1, seriesName = name)
        val seriesData = SeriesData(series)
        val episode = SeriesEpisode(id = 1, airedSeason = seasonN,
                airedEpisodeNumber = episodeN, episodeName = episodeName)
        val episodeData = SeriesEpisodeData(listOf(episode))
        val imageData = SeriesImageQueryData(emptyList())
        val seriesMeta = series.toTvSeriesRef(null, null)
        val episodeRef = episode.toTvEpisodeRef(1, null, null)

        whenever(mApi.refreshToken(mToken))
                .thenReturn(Single.just(mToken))
        whenever(mApi.searchSeries(mToken, name))
                .thenReturn(Single.just(seriesSearchData))
        whenever(mApi.series(mToken, 1))
                .thenReturn(Single.just(seriesData))
        whenever(mApi.seriesEpisodes(mToken, 1))
                .thenReturn(Single.just(episodeData))
        whenever(mApi.seriesImagesQuery(eq(mToken), eq(1),  Mockito.anyString()))
                .thenReturn(Single.just(imageData))

        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mClient.getTvEpisodesForTvSeries(seriesMeta.id)).thenAnswer(object : Answer<Observable<TvEpisodeRef>> {
            var times = 0
            override fun answer(invocation: InvocationOnMock?): Observable<TvEpisodeRef> {
                return if (times++ == 0) {
                    Observable.empty()
                } else {
                    Observable.just(episodeRef)
                }
            }
        })

        val item = mLookup.lookupObservable(meta).blockingFirst()
        assertThat(item).isNotNull()
        assertThat(item).isSameAs(episodeRef)

        verify(mApi).refreshToken(mToken)
        verify(mApi).searchSeries(mToken, name)
        verify(mApi).series(mToken, 1)
        verify(mApi).seriesEpisodes(mToken, 1)
        verify(mApi, times(3)).seriesImagesQuery(eq(mToken), eq(1), Mockito.anyString())

        verify(mVideoClient).getTvToken()
        verify(mClient).addTvSeries(any())
        verify(mClient, times(2)).getTvEpisodesForTvSeries(seriesMeta.id)

        //non mocked interactions
        verify(mVideoClient).setTvToken(mToken)
        verify(mClient, times(3)).addTvImages(emptyList())
        verify(mClient).addTvEpisodes(any())

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }

    @Test
    fun lookup_in_db_does_not_refetch() {
        val series = tvSeries()
        val episode = tvEpisode()

        val meta = LookupRequest(upnpVideo_folder_1_no_association())
        meta.lookupName = series.meta.title
        meta.seasonNumber = episode.meta.seasonNumber
        meta.episodeNumber = episode.meta.episodeNumber

        val searchResult = SeriesSearchData(listOf(SeriesSearch(id = series.id.seriesId)))

        whenever(mVideoClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mApi.refreshToken(mToken))
                .thenReturn(Single.just(mToken))
        whenever(mApi.searchSeries(mToken, meta.lookupName))
                .thenReturn(Single.just(searchResult))
        whenever(mClient.getTvEpisodesForTvSeries(series.id))
                .thenReturn(Observable.just(episode))

        val item = mLookup.lookupObservable(meta).blockingFirst()
        assertThat(item).isNotNull()
        assertThat(item).isSameAs(episode)

        verify(mVideoClient).getTvToken()
        verify(mApi).refreshToken(mToken)
        verify(mVideoClient).setTvToken(mToken)
        verify(mApi).searchSeries(mToken, meta.lookupName)
        verify(mClient).getTvEpisodesForTvSeries(series.id)

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
        verifyNoMoreInteractions(mVideoClient)
    }
}