package org.opensilk.media

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.assertj.core.api.Java6Assertions.assertThat
import org.opensilk.media.*

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun testnewMediaRefJsonSerialization() {
        val ref = MediaRef(UPNP_DEVICE, "this is an id")
        val json = ref.toJson()
        val newRef = newMediaRef(json)
        assertThat(newRef.kind).isEqualTo(UPNP_DEVICE)

        val f_ref = MediaRef(UPNP_FOLDER, FolderId("foobag", "barnfoo"))
        val f_json = f_ref.toJson()
        val f_newRef = newMediaRef(f_json)
        assertThat(f_newRef.kind).isEqualTo(UPNP_FOLDER)

        val u_ref = MediaRef(UPNP_VIDEO, UpnpItemId("foobag", "barnfoo"))
        val u_json = u_ref.toJson()
        val u_newRef = newMediaRef(u_json)
        assertThat(u_newRef.kind).isEqualTo(UPNP_VIDEO)
    }

    @Test
    fun testFolderIdJsonSerialization() {
        val ref = FolderId("foobag", "barnfoo")
        val json = ref.id
        val newRef = newFolderId(json)
        assertThat(newRef.deviceId).isEqualTo("foobag")
        assertThat(newRef.folderId).isEqualTo("barnfoo")
    }

    @Test
    fun testUpnpItemIdJsonSerialization() {
        val ref = UpnpItemId("foobag", "barnfoo")
        val json = ref.id
        val newRef = newUpnpItemId(json)
        assertThat(newRef.deviceId).isEqualTo("foobag")
        assertThat(newRef.itemId).isEqualTo("barnfoo")
    }

}