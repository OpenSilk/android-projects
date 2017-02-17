package org.opensilk.music.data

import android.content.ContentProvider
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsProvider
import android.util.JsonReader

import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowContentResolver

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.HashMap

import timber.log.Timber

/**
 * Created by drew on 6/27/16.
 */
class TestDataProvider : DocumentsProvider() {

    @Throws(FileNotFoundException::class)
    override fun queryRoots(projection: Array<String>): Cursor? {
        return null
    }

    @Throws(FileNotFoundException::class)
    override fun queryDocument(documentId: String, projection: Array<String>): Cursor {
        val m = MatrixCursor(projection)
        for (item in TEST_DATA) {
            if (documentId == item["document_id"].toString()) {
                val rb = m.newRow()
                for (col in projection) {
                    rb.add(col, item[col])
                }
            }
        }
        return m
    }

    @Throws(FileNotFoundException::class)
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>, sortOrder: String): Cursor {
        val m = MatrixCursor(projection)
        for (item in TEST_DATA) {
            if (parentDocumentId == item["parent_document_id"].toString()) {
                val rb = m.newRow()
                for (col in projection) {
                    rb.add(col, item[col])
                }
            }
        }
        return m
    }

    @Throws(FileNotFoundException::class)
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal): ParcelFileDescriptor {
        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val res = javaClass.classLoader.getResourceAsStream(documentId)
                    ?: throw FileNotFoundException("InputStream was null")
            val out = ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])
            Thread(Runnable {
                try {
                    IOUtils.copy(res, out)
                    out.flush()
                } catch (e: IOException) {
                    Timber.e(e, "Copy to pipe")
                } finally {
                    IOUtils.closeQuietly(res)
                    IOUtils.closeQuietly(out)
                }
            }).start()
            return pipe[0]
        } catch (e: IOException) {
            throw FileNotFoundException(e.message)
        }

    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        for (item in TEST_DATA) {
            if (documentId == item["document_id"].toString()
                    && parentDocumentId == item["parent_document_id"].toString()) {
                return true
            }
        }
        return false
    }


    private lateinit var TEST_DATA: List<Map<String, Any>>

    override fun onCreate(): Boolean {
        TEST_DATA = loadTestData()
        return true
    }

    internal fun loadTestData(): List<Map<String, Any>> {
        val testData = ArrayList<Map<String, Any>>()
        val jr = JsonReader(InputStreamReader(javaClass.classLoader.getResourceAsStream("test_data.json")))
        jr.beginArray()
        while (jr.hasNext()) {
            val entry = HashMap<String, Any>()
            jr.beginObject()
            while (jr.hasNext()) {
                val name = jr.nextName();
                when(name) {
                    "parent_document_id", "_display_name", "document_id",
                    "mime_type" -> entry[name] = jr.nextString()
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

    fun loadTestMeta(jsonReader: JsonReader): MediaMeta {
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

    companion object {

        val AUTHORITY = "foo.test.documents"

        @Throws(Exception::class)
        fun setup(): TestDataProvider {
            val provider = TestDataProvider()

            val pi = ProviderInfo()
            pi.authority = AUTHORITY
            pi.exported = true
            pi.grantUriPermissions = true
            pi.writePermission = android.Manifest.permission.MANAGE_DOCUMENTS
            pi.readPermission = android.Manifest.permission.MANAGE_DOCUMENTS

            //setups the provider
            provider.attachInfo(RuntimeEnvironment.application, pi)
            //run again to disable permissions
            val attachInfo = ContentProvider::class.java.getDeclaredMethod("attachInfo",
                    Context::class.java, ProviderInfo::class.java, Boolean::class.java)
            attachInfo.isAccessible = true
            attachInfo.invoke(provider, RuntimeEnvironment.application, pi, true)

            ShadowContentResolver.registerProvider(AUTHORITY, provider)
            return provider
        }
    }

}
