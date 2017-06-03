package org.opensilk.video.telly

import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun testMediaRefJsonSerialization() {
        val ref = MediaRef(UPNP_DEVICE, "this is an id")
        val json = ref.toJson()
        val newRef = newMediaRef(json)
        Assertions.assertThat(newRef.kind).isEqualTo(UPNP_DEVICE)
        Assertions.assertThat(newRef.mediaId).isEqualTo(StringId("this is an id"))
    }

    @Test
    fun testFolderIdJsonSerialization() {
        val ref = FolderId("foobag", "barnfoo")
        val json = ref.id
        val newRef = newFolderId(json)
        Assertions.assertThat(newRef.deviceId).isEqualTo("foobag")
        Assertions.assertThat(newRef.folderId).isEqualTo("barnfoo")
    }

}