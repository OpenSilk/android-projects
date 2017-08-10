package org.opensilk.music.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.browse.MediaBrowser
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.ServiceScope
import org.opensilk.media._getMediaUri
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by drew on 6/26/16.
 */
interface MetaExtactor {

    fun extractMeta(mediaItem: MediaBrowser.MediaItem): rx.Single<MediaBrowser.MediaItem>

    @ServiceScope
    class Default
    @Inject constructor(
            @ForApplication private val mContext: Context
    ): MetaExtactor {

        companion object {

            private val BLANK_HEADERS = emptyMap<String, String>()

            @Throws(NumberFormatException::class)
            internal fun parseTrackNum(track_num: String?): Int {
                if (StringUtils.contains(track_num, "/")) {
                    return Integer.parseInt(StringUtils.split(track_num, "/")[0])
                } else {
                    return Integer.parseInt(track_num)
                }
            }

            @Throws(NumberFormatException::class)
            internal fun parseDiskNum(disc_num: String?): Int {
                if (StringUtils.contains(disc_num, "/")) {
                    return Integer.parseInt(StringUtils.split(disc_num, "/")[0])
                } else {
                    return Integer.parseInt(disc_num)
                }
            }

            //TODO how to handle this properly???
            internal fun fixLatin1(string: String?): String {
                return neverNull(string?.replace("â€™", "’"))
            }

            internal fun neverNull(str: String?): String {
                return str ?: ""
            }
        }

        override fun extractMeta(mediaItem: MediaBrowser.MediaItem): Single<MediaBrowser.MediaItem> {
            val doExtract = { mmr: MediaMetadataRetriever -> single<MediaBrowser.MediaItem> { try {

                val mediaUri = mediaItem._getMediaUri()
                if (StringUtils.startsWith(mediaUri.scheme, "http")) {
                    mmr.setDataSource(mediaUri.toString(), BLANK_HEADERS)
                } else if (StringUtils.equals(mediaUri.scheme, "content")) {
                    mmr.setDataSource(mContext, mediaUri)
                } else if (StringUtils.equals(mediaUri.scheme, "file")) {
                    mmr.setDataSource(mediaUri.path)
                } else {
                    throw IllegalArgumentException("Unknown scheme " + mediaUri.scheme)
                }

                val bob = mediaItem.description._newBuilder()
                val meta = mediaItem._getMediaMeta()

                val title = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                if (title != "") {
                    meta.title = title
                    bob.setTitle(title)
                }
                val albumName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                if (albumName != "") {
                    meta.albumName = albumName
                }
                val albumArtistName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                if (albumArtistName != "") {
                    meta.albumArtistName = albumArtistName
                }
                val artistName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                if (artistName != "") {
                    meta.artistName = artistName
                    bob.setSubtitle(artistName)
                }
                val genreName = fixLatin1(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                if (genreName != "") {
                    meta.genreName = genreName
                }
                val mimeType = neverNull(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
                if (mimeType != "") {
                    meta.mimeType = mimeType
                }
                try {
                    meta.releaseYear = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toInt() ?: -1
                } catch (e: NumberFormatException) {
                    Timber.w(e, "extract releaseYear")
                }
                try {
                    meta.bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLong() ?: -1
                } catch (e: NumberFormatException) {
                    Timber.w(e, "extract bitrate")
                }
                try {
                    meta.trackNumber = parseTrackNum(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER))
                } catch(e: NumberFormatException) {
                    Timber.w(e, "extract trackNumber")
                }
                try {
                    meta.discNumber = parseDiskNum(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER))
                } catch(e: NumberFormatException) {
                    Timber.w(e, "extract discNumber")
                }
                try {
                    meta.isCompilation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)?.toInt() ?: 0 != 0
                } catch (e: NumberFormatException) {
                    Timber.w(e, "extract isCompilation")
                }
                try {
                    meta.duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: -1
                } catch (e: NumberFormatException) {
                    Timber.w(e, "extract duration")
                }

                val flds = MediaMetadataRetriever::class.java.declaredFields
                val sb = StringBuilder(100)
                for (f in flds) {
                    if (f.name.startsWith("METADATA_KEY")) {
                        sb.append(f.name).append(": ").append(mmr.extractMetadata(f.getInt(null))).append("\n")
                    }
                }
                Timber.i(sb.toString())

                it.onSuccess(newMediaItem(bob, meta))
            } catch (e: Exception) {
                it.onError(e)
            } } }
            return Single.using<MediaBrowser.MediaItem, MediaMetadataRetriever>(
                    { MediaMetadataRetriever() },
                    doExtract,
                    { it.release() }
            )
        }

    }
}