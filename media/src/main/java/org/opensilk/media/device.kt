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
interface MediaDeviceMeta {
    val title: String
}

/**
 * Basic media device
 */
interface MediaDeviceRef: MediaRef {
    override val id: MediaDeviceId
    val meta: MediaDeviceMeta
}