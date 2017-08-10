package org.opensilk.playback

import android.os.Looper
import android.provider.DocumentsContract
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.music.MusicDbClient
import org.opensilk.music.MusicDbUris
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.playback.PlaybackSession
import rx.schedulers.Schedulers

/**
 * Created by drew on 3/14/17.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceTest {

    internal lateinit var mPB: PlaybackSession

    @Before
    fun setUp() {
        Log.e("SETUP", "SETUP")
        Looper.prepare()
        val context = InstrumentationRegistry.getTargetContext()
        mPB = PlaybackSession(context, MusicDbClient(context,
                MusicDbUris("foo")), Schedulers.immediate())
    }

    @After
    fun tearDown() {
        mPB.release()
    }

    @Test
    fun doSomething() {
        val doc = DocumentRef(DocumentsContract.buildTreeDocumentUri("foo.test.provider", "Music"), "foo")
        mPB.onPlayFromMediaId(doc.mediaId, null)
    }

}