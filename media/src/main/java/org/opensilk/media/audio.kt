package org.opensilk.media

/**
 * Created by drew on 9/6/17.
 */
interface AudioId: MediaId

interface AudioMeta: MediaMeta

interface AudioRef: MediaRef {
    override val id: AudioId
    override val meta: AudioMeta
}
