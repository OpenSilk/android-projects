package org.opensilk.video

import android.net.Uri
import com.nhaarman.mockito_kotlin.*
import io.reactivex.Maybe
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.opensilk.tvdb.api.TVDb
import org.opensilk.tvdb.api.model.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.reactivex.Observable
import io.reactivex.Single
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.opensilk.media.*

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
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
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
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
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
                .thenReturn(Single.just(mToken))

        val tkn = mLookup.mTokenObservable.blockingFirst()
        assertThat(tkn).isSameAs(mToken)

        verify(mApi).refreshToken(mToken)
        verify(mClient).getTvToken()
        verify(mClient).setTvToken(mToken)

        val tkn2 = mLookup.mTokenObservable.blockingFirst()
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

        val meta = LookupRequest(upnpVideo_folder_1_no_association())
        meta.lookupName = name
        meta.seasonNumber = seasonN
        meta.episodeNumber = episodeN
        val mediaRef = meta.mediaRef as UpnpVideoRef

        val seriesSearch = SeriesSearch(id = 1, seriesName = name)
        val seriesSearchData = SeriesSearchData(listOf(seriesSearch))
        val series = Series(id = 1, seriesName = name)
        val seriesData = SeriesData(series)
        val episode = SeriesEpisode(id = 1, airedSeason = seasonN,
                airedEpisodeNumber = episodeN, episodeName = episodeName)
        val episodeData = SeriesEpisodeData(listOf(episode))
        val imageData = SeriesImageQueryData(emptyList())
        val seriesUri = Uri.parse("foo.com/1")
        val seriesMeta = series.toTvSeriesRef(null, null)

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

        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mClient.addTvSeries(any()))
                .thenReturn(seriesUri)
        whenever(mClient.getTvEpisodesForTvSeries(seriesMeta.id)).thenAnswer(object : Answer<Maybe<UpnpVideoRef>> {
            var times = 0
            override fun answer(invocation: InvocationOnMock?): Maybe<UpnpVideoRef> {
                if (times++ == 0) {
                    return Maybe.empty()
                } else {
                    return Maybe.just(mediaRef)
                }
            }
        })

        val list = mLookup.lookupObservable(meta).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(seriesMeta)

        verify(mApi).refreshToken(mToken)
        verify(mApi).searchSeries(mToken, name)
        verify(mApi).series(mToken, 1)
        verify(mApi).seriesEpisodes(mToken, 1)
        verify(mApi, times(2)).seriesImagesQuery(eq(mToken), eq(1), Mockito.anyString())

        verify(mClient).getTvToken()
        verify(mClient).addTvSeries(any())
        verify(mClient, times(2)).getTvEpisodesForTvSeries(seriesMeta.id)

        //non mocked interactions
        verify(mClient).setTvToken(mToken)
        verify(mClient, times(2)).addTvImages(emptyList())
        verify(mClient).addTvEpisodes(any())

        verifyNoMoreInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }

    @Test
    fun lookup_with_association_does_not_touch_network() {
        val series = tvSeries()
        val episode = tvEpisode()

        val meta = LookupRequest(upnpVideo_folder_1_no_association())
        meta.lookupName = series.meta.title
        meta.seasonNumber = episode.meta.seasonNumber
        meta.episodeNumber = episode.meta.episodeNumber

        val metaRef = meta.mediaRef as UpnpVideoRef

        whenever(mClient.getTvToken())
                .thenReturn(Single.just(mToken))
        whenever(mClient.getTvEpisodesForTvSeries(series.id))
                .thenReturn(Observable.just(episode))

        val list = mLookup.lookupObservable(meta).toList().blockingGet()
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isSameAs(episode)

        verify(mClient).getTvToken()
        verify(mClient).getTvEpisodesForTvSeries(series.id)

        verifyZeroInteractions(mApi)
        verifyNoMoreInteractions(mClient)
    }
}