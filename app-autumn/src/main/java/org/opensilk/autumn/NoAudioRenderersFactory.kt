package org.opensilk.autumn

import android.content.Context
import android.os.Handler
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import java.util.ArrayList

/**
 * Created by drew on 8/16/17.
 */
class NoAudioRenderersFactory(context: Context): DefaultRenderersFactory(context) {

    override fun buildAudioRenderers(context: Context?,
                                     drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
                                     audioProcessors: Array<out AudioProcessor>?,
                                     eventHandler: Handler?,
                                     eventListener: AudioRendererEventListener?,
                                     extensionRendererMode: Int,
                                     out: ArrayList<Renderer>?) {
        //pass
    }
}