package org.opensilk.music.data

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import android.support.test.InstrumentationRegistry
import android.util.JsonReader
import org.opensilk.media.DocumentRef
import java.io.InputStreamReader
import java.util.*

/**
 * Created by drew on 8/2/16.
 */
object TestDocuments {

    val sItems: Map<String, MediaBrowser.MediaItem> = emptyMap()
    /*
    private val sAccessUri = DocumentsContract.buildTreeDocumentUri("foo.test.provider", "Music")

    val sItems: Map<String, MediaBrowser.MediaItem> by lazy {
        return@lazy HashMap<String, MediaBrowser.MediaItem>().apply {
            loadTestData().forEach {
                val bob = MediaDescription.Builder()
                when (it["kind"]) {
                    "directory" -> {
                        bob.setMediaId(DocumentRef(sAccessUri, it["document_id"] as String).mediaId)
                        bob.setTitle(it["_display_name"] as String)
                        meta.displayName = it["_display_name"] as String
                        meta.mimeType = it["mime_type"] as String
                        meta.size = it["_size"] as Long
                        meta.parentMediaId = DocumentRef(sAccessUri, it["parent_document_id"] as String).mediaId
                        put(it["document_id"] as String, newMediaItem(bob, meta))
                    }
                    "track" -> {
                        val meta = it["meta"] as MediaMeta
                        bob.setMediaId(DocumentRef(sAccessUri, it["document_id"] as String).mediaId)
                        bob.setTitle(it["_display_name"] as String)
                        meta.displayName = it["_display_name"] as String
                        meta.mimeType = it["mime_type"] as String
                        meta.size = it["_size"] as Long
                        meta.parentMediaId = DocumentRef(sAccessUri, it["parent_document_id"] as String).mediaId
                        put(it["document_id"] as String, newMediaItem(bob, meta))
                    }
                }

            }
        }
    }

    fun getItem(baseUri: String, documentId: String): MediaBrowser.MediaItem {
        val item = sItems[documentId]!!
        val bob = item.description._newBuilder()
        val meta = item._getMediaMeta()
        bob._setMediaUri(meta, Uri.parse(baseUri))//so we can use mockwebserver
        return newMediaItem(bob, meta)
    }

    private fun loadTestData(): List<Map<String, Any>> {
        val testData = ArrayList<Map<String, Any>>()
        val jr = JsonReader(InputStreamReader(
                InstrumentationRegistry.getContext().assets.open("test_data.json")))
        jr.beginArray()
        while (jr.hasNext()) {
            val entry = HashMap<String, Any>()
            jr.beginObject()
            while (jr.hasNext()) {
                val name = jr.nextName()
                when(name) {
                    "parent_document_id", "_display_name", "document_id",
                    "mime_type", "kind" -> entry[name] = jr.nextString()
                    "_size" -> entry[name] = jr.nextLong()
                    "meta" -> entry[name] = loadTestMeta(jr)
                    else -> jr.skipValue()
                }
            }
            jr.endObject()
            testData.add(entry)
        }
        jr.endArray()
        jr.close()
        return testData
    }

    private fun loadTestMeta(jsonReader: JsonReader): MediaMeta {
        val m = MediaMeta.empty()
        jsonReader.beginObject()
        while (jsonReader.hasNext()) {
            when (jsonReader.nextName()) {
                "album" -> m.albumName = jsonReader.nextString()
                "album_artist" -> m.albumArtistName = jsonReader.nextString()
                "artist" -> m.artistName = jsonReader.nextString()
                "bitrate" -> m.bitrate = jsonReader.nextLong()
                "track_number" -> m.trackNumber = jsonReader.nextInt()
                "compilation" -> m.isCompilation = jsonReader.nextBoolean()
                "disc_number" -> m.discNumber = jsonReader.nextInt()
                "duration" -> m.duration = jsonReader.nextLong()
                "genre" -> m.genreName = jsonReader.nextString()
                "mime" -> m.mimeType = jsonReader.nextString()
                "title" -> m.title = jsonReader.nextString()
                else -> jsonReader.skipValue()
            }
        }
        jsonReader.endObject()
        return m
    }
    */
}