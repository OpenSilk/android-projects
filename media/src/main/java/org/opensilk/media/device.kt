package org.opensilk.media

/**
 * Device classification
 */
interface MediaDeviceId: MediaId

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