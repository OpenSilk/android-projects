package org.opensilk.media.playback

import android.net.Uri
import io.reactivex.Observable

/**
 * Created by drew on 3/16/17.
 */
interface Renderer {

    val state: Int
    val stateChanges: Observable<Int>
    val hasNext: Boolean

    /**
     * State will be BUFFERING or ERROR after call
     */
    fun loadMedia(uri: Uri, headers: Map<String, String> = emptyMap())

    /**
     *
     */
    fun loadNextMedia(uri: Uri, headers: Map<String, String> = emptyMap())

    /**
     * If state is BUFFERING, will transition to PLAYING once ready
     * If state is PAUSED, will transition to PLAYING
     * If state is PLAYING, does nothing
     *
     */
    fun play()

    /**
     * If state is BUFFERING, will transition to PAUSED once ready
     * If state is PAUSED, does nothing
     * If state is PLAYING, will transition to PAUSED
     */
    fun pause()

    /**
     *
     */
    fun seekTo()

    fun release()
}
