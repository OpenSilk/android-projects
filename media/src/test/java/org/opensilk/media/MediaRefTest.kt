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
    fun testUpnpDeviceIdSerialization() {
        val ref = UpnpDeviceId("this is an id")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun testUpnpFolderIdJsonSerialization() {
        val ref = UpnpFolderId("foobag", "barnfoo")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun testUpnpItemIdJsonSerialization() {
        val ref = UpnpVideoId("foobag", "barnfoo")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

}