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
    fun docDirectoryId_toJson() {
        val ref = DocDirectoryId(Uri.parse("content://foo/tree/myid"), "myid", "ooo")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun docVideoId_toJson() {
        val ref = DocVideoId(Uri.parse("content://foo/tree/mvid"), "foo", "mmm")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun storageDeviceId_toJson() {
        val ref = StorageDeviceId("andountu", "bouneot", true)
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun storageFolderId_toJson() {
        val ref = StorageFolderId("onut", "ontue", "ouou")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

    @Test
    fun storageVideoId_toJson() {
        val ref = StorageVideoId("ontu", "eontu", "ountou")
        val newRef = parseMediaId(ref.json)
        assertThat(newRef).isEqualTo(ref)
    }

}