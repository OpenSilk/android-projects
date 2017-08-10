package org.opensilk.music

import android.content.ContentResolver
import android.content.UriMatcher
import android.net.Uri
import javax.inject.Inject
import javax.inject.Named

/**
 *
 */
class MusicDbUris
@Inject
constructor(
        @Named("music_authority") private val mAuthority: String
){

    private fun base(): Uri.Builder {
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(mAuthority).appendPath("music")
    }

    fun mediaDocs(): Uri {
        return base().appendPath("mediadocs").build()
    }

    val matcher: UriMatcher by lazy {
        val m = UriMatcher(UriMatcher.NO_MATCH)
        m.addURI(mAuthority, "music/mediadocs", MATCH_MEDIA_DOC)
        m //return
    }

    companion object {
        val MATCH_MEDIA_DOC = 10
    }
}