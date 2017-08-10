package org.opensilk.music.data

import android.provider.DocumentsContract

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

import timber.log.Timber

import org.assertj.core.api.Assertions.*
import org.opensilk.media.MediaMeta
import org.opensilk.music.BuildConfig
import org.opensilk.music.MusicDbProvider
import org.opensilk.music.MusicDbClient
import org.opensilk.music.MusicDbUris
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MediaRef
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner


/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class,
        sdk = intArrayOf(21))
class MusicDbProviderTest {
    internal lateinit var mMusicDbProvider: MusicDbProvider
    internal lateinit var mClient: MusicDbClient

    @Before
    @Throws(Exception::class)
    fun setup() {
        Timber.uprootAll()
        Timber.plant(Timber.DebugTree())
        ShadowLog.stream = System.err
        mMusicDbProvider = Robolectric.buildContentProvider(MusicDbProvider::class.java).create().get()

//        val attachInfo = ContentProvider::class.java.getDeclaredMethod("attachInfo",
//                Context::class.java, ProviderInfo::class.java, Boolean::class.java)
//        attachInfo.isAccessible = true
//        attachInfo.invoke(mMusicProvider, RuntimeEnvironment.application, null, true)

        val authority = MusicAuthorityModule().provideMusicAuthority(RuntimeEnvironment.application)
        mClient = MusicDbClient(RuntimeEnvironment.application, MusicDbUris(authority))

        TestDataProvider.setup()
    }


    @Test
    fun test_rootOps() {
        mClient.insertRootDoc(DocumentRef.root(DocumentsContract.buildTreeDocumentUri(TestDataProvider.AUTHORITY, "Music")))
        assertThat(mClient.getRootDocs()).hasSize(1)
        //get back
        val mediaItem = mClient.getRootDocs()[0]
        assertThat(mediaItem.description.title).isEqualTo("Music")
        val mediaMeta = MediaMeta.from(mediaItem.description)
        assertThat(mediaMeta.displayName).isEqualTo("Music")
        assertThat(mediaMeta.size).isEqualTo(1275)
        assertThat(mediaMeta.isDirectory).isTrue()
        //remove
        val removed = mClient.removeRootDoc(MediaRef.parse(mediaItem.mediaId) as DocumentRef)
        assertThat(removed).isTrue()
        assertThat(mClient.getRootDocs()).isEmpty()
    }
}