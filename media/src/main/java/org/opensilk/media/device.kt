package org.opensilk.media

/**
 * Device classification
 */
interface MediaDeviceId: MediaId

object NoMediaDeviceId: MediaDeviceId {
    override val json: String = ""
}

/**
 * Basic meta for [MediaDeviceRef]
 */
interface MediaDeviceMeta: MediaMeta

/**
 * Basic media device
 */
interface MediaDeviceRef: MediaRef {
    override val id: MediaDeviceId
    override val meta: MediaDeviceMeta
}