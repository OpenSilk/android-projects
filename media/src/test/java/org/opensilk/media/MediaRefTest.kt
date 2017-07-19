package org.opensilk.media

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun testUpnpDeviceSerialization() {
        val ref = MediaRef(UPNP_DEVICE, UpnpDeviceId("this is an id"))
        val newRef = newMediaRef(ref.toJson())
        assertThat(newRef.kind).isEqualTo(UPNP_DEVICE)
        assertThat((newRef.mediaId as UpnpDeviceId).deviceId).isEqualTo("this is an id")
    }

    @Test
    fun testFolderIdJsonSerialization() {
        val ref = MediaRef(UPNP_FOLDER, UpnpFolderId("foobag", "barnfoo"))
        val newRef = newMediaRef(ref.toJson())
        assertThat(newRef.kind).isEqualTo(UPNP_FOLDER)
        assertThat((newRef.mediaId as UpnpFolderId).deviceId).isEqualTo("foobag")
        assertThat((newRef.mediaId as UpnpFolderId).folderId).isEqualTo("barnfoo")
    }

    @Test
    fun testUpnpItemIdJsonSerialization() {
        val ref = MediaRef(UPNP_VIDEO, UpnpVideoId("foobag", "barnfoo"))
        val newRef = newMediaRef(ref.toJson())
        assertThat(newRef.kind).isEqualTo(UPNP_VIDEO)
        assertThat((newRef.mediaId as UpnpVideoId).deviceId).isEqualTo("foobag")
        assertThat((newRef.mediaId as UpnpVideoId).itemId).isEqualTo("barnfoo")
    }

}