package org.opensilk.video.telly

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import org.assertj.core.api.Java6Assertions.assertThat

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun testMediaRefJsonSerialization() {
        val ref = MediaRef(UPNP_DEVICE, "this is an id")
        val json = ref.toJson()
        val newRef = newMediaRef(json)
        assertThat(newRef.kind).isEqualTo(UPNP_DEVICE)
        assertThat(newRef.mediaId).isEqualTo(StringId("this is an id"))
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