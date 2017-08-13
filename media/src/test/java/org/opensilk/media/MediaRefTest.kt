package org.opensilk.media

import android.net.Uri
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class MediaRefTest {

    @Test
    fun upnpDeviceId_toJson() {
        val ref = UpnpDeviceId("this is an id")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun upnpFolderId_toJson() {
        val ref = UpnpFolderId("foobag", "bor", "barnfoo")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun upnpVideoId_toJson() {
        val ref = UpnpVideoId("foobag", "bov", "barnfoo")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun upnpAudioId_toJson(){
        val ref = UpnpAudioId("foo", "foo1", "foob")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun upnpMusicTrackId_toJson() {
        val ref = UpnpMusicTrackId("foo", "foo1", "foob")
        val newref = parseMediaId(ref.json)
        assertThat(newref).isEqualTo(ref)
    }

    @Test
    fun documentId_toJson() {
        val ref = DocumentId(Uri.parse("content://foo/tree/myid"), "myid", "ooo", "mime/type")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

}