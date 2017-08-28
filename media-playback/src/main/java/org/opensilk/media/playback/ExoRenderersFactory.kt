package org.opensilk.media.playback

import android.content.Context
import android.os.Build
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.util.MimeTypes
import timber.log.Timber
import java.util.ArrayList

/**
 * Created by drew on 8/28/17.
 */
class ExoRenderersFactory(context: Context): DefaultRenderersFactory(
        context, null, DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON) {

    override fun buildAudioRenderers(context: Context, drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     audioProcessors: Array<out AudioProcessor>, eventHandler: Handler,
                                     eventListener: AudioRendererEventListener, extensionRendererMode: Int,
                                     out: ArrayList<Renderer>) {
        out.add(ExoMediaCodecAudioRenderer(context, drmSessionManager,
                audioProcessors, eventHandler, eventListener))
        out.add(FfmpegAudioRenderer(eventHandler, eventListener, *audioProcessors))
    }
}

class ExoMediaCodecAudioRenderer(
        val context: Context, drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
        audioProcessors: Array<out AudioProcessor>, eventHandler: Handler,
        eventListener: AudioRendererEventListener
): MediaCodecAudioRenderer(
        MediaCodecSelector.DEFAULT, drmSessionManager, true,
        eventHandler, eventListener, AudioCapabilities.getCapabilities(context), *audioProcessors
) {

    val isFugu = "fugu" == Build.DEVICE

    override fun allowPassthrough(mimeType: String): Boolean {
        Timber.d("allowPassthrough($mimeType) caps=${AudioCapabilities.getCapabilities(context)} isFugu=$isFugu")
        return if (isFugu && mimeType == MimeTypes.AUDIO_AC3) {
            Timber.w("Disabling broken hdmi audio passthrough on fugu")
            false
        } else super.allowPassthrough(mimeType)
    }

}