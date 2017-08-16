package org.opensilk.autumn

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import io.reactivex.disposables.CompositeDisposable
import okhttp3.OkHttpClient
import org.opensilk.dagger2.ForApp
import timber.log.Timber
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject

@Module
abstract class DreamViewModelModule {
    @Binds @IntoMap @ViewModelKey(DreamViewModel::class)
    abstract fun provideViewModel(dreamViewModel: DreamViewModel): ViewModel
}

/**
 * Created by drew on 8/4/17.
 */
class DreamViewModel
@Inject constructor(
        @ForApp private val mContext: Context,
        private val mDataService: DataService,
        private val mPrefs: SharedPreferences,
        okHttpClient: OkHttpClient
): ViewModel(), Player.EventListener, SimpleExoPlayer.VideoListener {

    private val mDataSourceFactory = DefaultDataSourceFactory(mContext,
            null, OkHttpDataSourceFactory(okHttpClient, "${mContext.packageName}/ExoPlayer", null))
    private val mExtractorFactory = DefaultExtractorsFactory()
    private val mTrackSelector: DefaultTrackSelector = DefaultTrackSelector()
    private val mRenderersFactory = NoAudioRenderersFactory(mContext)
    private var mExoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(mRenderersFactory, mTrackSelector)

    private val mDisposables = CompositeDisposable()
    private val mQueue = ArrayList<Playlist>()
    private var mCurrentIdx = -1
    private var mCurrentWindow = -1

    val aspectRatio = MutableLiveData<Float>()
    val loading = MutableLiveData<Boolean>()
    val label = MutableLiveData<String>()

    init {
        val logger = EventLogger(mTrackSelector)
        mExoPlayer.addListener(logger)
        mExoPlayer.setAudioDebugListener(logger)
        mExoPlayer.setVideoDebugListener(logger)
        mExoPlayer.setMetadataOutput(logger)

        //for tunneled video
        mTrackSelector.setTunnelingAudioSessionId(C.generateAudioSessionIdV21(mContext))
    }

    override public fun onCleared() {
        super.onCleared()
        mDisposables.clear()
        mExoPlayer.release()
    }

    fun setSurface(surfaceView: SurfaceView) {
        mExoPlayer.setVideoSurfaceView(surfaceView)
        mExoPlayer.addListener(this)
        mExoPlayer.setVideoListener(this)
        loadPlaylist()
    }

    fun releaseSurface(surfaceView: SurfaceView) {
        mDisposables.clear()
        mExoPlayer.clearVideoSurfaceView(surfaceView)
        mExoPlayer.removeListener(this)
        mExoPlayer.clearVideoListener(this)
    }

    fun play() {
        mExoPlayer.playWhenReady = true
    }

    fun pause() {
        mExoPlayer.playWhenReady = false
    }

    private fun loadPlaylist() {
        val s = mDataService.getPlaylists()
                .observeOn(AppSchedulers.ui)
                .subscribe({ list ->
                    mQueue.clear()
                    mQueue.addAll(list)
                    val lastPlaylist = mPrefs.getString("last_playlist", "")
                    mCurrentIdx = list.indexOfFirst { it.id == lastPlaylist }
                    goToNext()
                    preparePlaylist()
                }, {
                    throw it
                })
        mDisposables.add(s)
    }

    private fun preparePlaylist() {
        mExoPlayer.stop()
        if (mCurrentIdx < 0) {
            return
        }
        val playlist = mQueue[mCurrentIdx]
        val mediaList = Array(playlist.assets.size, { idx ->
                newMediaSource(Uri.parse(playlist.assets[idx].url))
        })
        Timber.d("preparing playlist %s", playlist)
        mExoPlayer.prepare(ConcatenatingMediaSource(*(mediaList)))
    }

    private fun newMediaSource(uri: Uri): MediaSource {
        return ExtractorMediaSource(uri, mDataSourceFactory, mExtractorFactory, null, null)
    }

    private fun saveCurrent() {
        val idx = mCurrentIdx
        if (idx >= 0 && idx < mQueue.size) {
            mPrefs.edit().putString("last_playlist", mQueue[idx].id).apply()
        }
    }

    private fun goToNext() {
        var next = mCurrentIdx + 1
        if (next < 0 || next >= mQueue.size) {
            next = 0
        }
        mCurrentIdx = next
        saveCurrent()
    }

    private fun maybeUpdateWindow() {
        if (mCurrentWindow != mExoPlayer.currentWindowIndex) {
            mCurrentWindow = mExoPlayer.currentWindowIndex
            val list = mQueue[mCurrentIdx]
            if (mCurrentWindow >= 0 && mCurrentWindow < list.assets.size) {
                label.value = list.assets[mCurrentWindow].accessibilityLabel
            }
        }
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
        maybeUpdateWindow()
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        TODO()
    }

    fun Int._stringifyExoPlayerState(): String = when (this) {
        Player.STATE_BUFFERING -> "STATE_BUFFERING"
        Player.STATE_IDLE -> "STATE_IDLE"
        Player.STATE_ENDED -> "STATE_ENDED"
        Player.STATE_READY -> "STATE_READY"
        else -> "UNKNOWN"
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Timber.d("onExoPlayerStateChanged(playWhenReady=%s, playbackState=%s",
                playWhenReady, playbackState._stringifyExoPlayerState())
        when (playbackState) {
            Player.STATE_IDLE -> {
            }
            Player.STATE_BUFFERING -> {
                loading.postValue(true)
            }
            Player.STATE_ENDED -> {
                goToNext()
                preparePlaylist()
                play()
            }
            Player.STATE_READY -> {
                loading.postValue(false)
                mExoPlayer.playWhenReady = true
            }
        }
    }

    override fun onLoadingChanged(isLoading: Boolean) {

    }

    override fun onPositionDiscontinuity() {
        maybeUpdateWindow()
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {

    }

    override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
        aspectRatio.postValue(if (height == 0) 1f else (width.toFloat() * pixelWidthHeightRatio) / height.toFloat())
    }

    override fun onRenderedFirstFrame() {
        loading.postValue(false)
    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }
}