package org.opensilk.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.browse.MediaBrowser
import android.net.Uri
import io.reactivex.Single
import org.apache.commons.lang3.StringUtils
import org.opensilk.dagger2.ForApp
import org.opensilk.media.MediaRef
import org.opensilk.media._getMediaUri
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 6/26/16.
 */
interface MetaExtactor {

    fun extractMeta(mediaItem: MediaBrowser.MediaItem): Single<MediaRef>

    class Default
    @Inject constructor(
            @ForApp private val mContext: Context
    ): MetaExtactor {

        companion object {

            private val BLANK_HEADERS = emptyMap<String, String>()

            @Throws(NumberFormatException::class)
            internal fun parseTrackNum(track_num: String?): Int = when {
                StringUtils.contains(track_num, "/") ->
                    Integer.parseInt(StringUtils.split(track_num, "/")[0])
                else ->
                    Integer.parseInt(track_num)
            }

            @Throws(NumberFormatException::class)
            internal fun parseDiskNum(disc_num: String?): Int = when {
                StringUtils.contains(disc_num, "/") ->
                    Integer.parseInt(StringUtils.split(disc_num, "/")[0])
                else ->
                    Integer.parseInt(disc_num)
            }

            //TODO how to handle this properly???
            internal fun fixLatin1(string: String?): String = neverNull(string?.replace("â€™", "’"))

            internal fun neverNull(str: String?): String = str ?: ""
        }

        override fun extractMeta(mediaItem: MediaBrowser.MediaItem): Single<MediaRef> {
            return Single.using<MediaRef, MediaMetadataRetriever>({
                MediaMetadataRetriever()
            }, { mmr ->
                Single.fromCallable { doExtract(mmr, mediaItem._getMediaUri()) }
            }, {
                it.release()
            })
        }

        fun doExtract(mmr: MediaMetadataRetriever, mediaUri: Uri): MediaRef {
            when {
                StringUtils.startsWith(mediaUri.scheme, "http") -> mmr.setDataSource(mediaUri.toString(), BLANK_HEADERS)
                StringUtils.equals(mediaUri.scheme, "content") -> mmr.setDataSource(mContext, mediaUri)
                StringUtils.equals(mediaUri.scheme, "file") -> mmr.setDataSource(mediaUri.path)
                else -> throw IllegalArgumentException("Unknown scheme " + mediaUri.scheme)
            }

            val title = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
            val albumName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            val albumArtistName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
            val artistName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            val genreName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
            val mimeType = neverNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
            var releaseYear = 0
            try {
                releaseYear = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toInt() ?: 0
            } catch (e: NumberFormatException) {
                Timber.w(e, "extract releaseYear")
            }
            var bitrate = 0L
            try {
                bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLong() ?: 0L
            } catch (e: NumberFormatException) {
                Timber.w(e, "extract bitrate")
            }
            var trackNumber = 0
            try {
                trackNumber = parseTrackNum(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
            } catch(e: NumberFormatException) {
                Timber.w(e, "extract trackNumber")
            }
            var discNumber = 0
            try {
                discNumber = parseDiskNum(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER))
            } catch(e: NumberFormatException) {
                Timber.w(e, "extract discNumber")
            }
            var isCompilation = false
            try {
                isCompilation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)?.toInt() ?: 0 != 0
            } catch (e: NumberFormatException) {
                Timber.w(e, "extract isCompilation")
            }
            var duration = 0L
            try {
                duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } catch (e: NumberFormatException) {
                Timber.w(e, "extract duration")
            }

            val flds = MediaMetadataRetriever::class.java.declaredFields
            val sb = StringBuilder(100)
            flds.filter { it.name.startsWith("METADATA_KEY") }.forEach {
                sb.append(it.name).append(": ").append(mmr.extractMetadata(it.getInt(null))).append("\n")
            }
            Timber.i(sb.toString())
            TODO()
        }
    }
}