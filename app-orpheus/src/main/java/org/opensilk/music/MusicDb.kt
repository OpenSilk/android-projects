package org.opensilk.music

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.ForApplication
import org.opensilk.common.dagger.ProviderScope
import org.opensilk.media.DocumentRef
import org.opensilk.music.data.MusicAuthorityModule
import java.util.*
import javax.inject.Inject

/**
 * Created by drew on 6/26/16.
 */
const val DATABASE_NAME = "music.sqlite"
const val DATABASE_VERSION = 4

val DOCUMENT_COLS = arrayOf(
    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
    DocumentsContract.Document.COLUMN_MIME_TYPE,
    DocumentsContract.Document.COLUMN_SIZE,
    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    DocumentsContract.Document.COLUMN_FLAGS
)

val ROOT_COLS = DOCUMENT_COLS + arrayOf(
        "authority",
        "parent_document_id",
        "tree_uri",
        "date_added",
        "title",
        "subtitle"
)

val MEDIA_COLS = ROOT_COLS + arrayOf(
        "album_name",
        "artist_name",
        "album_artist_name",
        "genre_name",
        "release_year",
        "bitrate",
        "duration",
        "compilation",
        "track_number",
        "disc_number",
        "artwork_uri",
        "backdrop_uri",
        "headers"
)

const val ROOTS_PARENT = "\u2605G\u2605O\u2605D\u2605"

fun Cursor.mapCols(cols: Array<String>): Map<String, Int> {
    val map = HashMap<String, Int>()
    for (col in cols) {
        val idx = this.getColumnIndex(col)
        if (idx >= 0) {
            map[col] = idx
        }
    }
    return map
}

/**
 * @param parentRef if certain columns are missing use values from it instead
 */
fun Cursor.makeDocMediaItem(colMap: Map<String, Int>, parentRef: DocumentRef? = null): MediaBrowser.MediaItem {

    val getLong = { col: String -> if (colMap[col] != null) this.getLong(colMap[col]!!) else null }
    val getInt = { col: String -> if (colMap[col] != null) this.getInt(colMap[col]!!) else null }
    val getString = { col: String -> if (colMap[col] != null) this.getString(colMap[col]!!) else null }

    val mediaMeta = MediaMeta()
    val mbob = MediaDescription.Builder()

    mediaMeta.mimeType = getString(DocumentsContract.Document.COLUMN_MIME_TYPE)!!
    mediaMeta.displayName = getString(DocumentsContract.Document.COLUMN_DISPLAY_NAME)!!
    mbob.setTitle(mediaMeta.displayName)

    val documentId = getString(DocumentsContract.Document.COLUMN_DOCUMENT_ID)!!
    val treeUri = getString("tree_uri")?.let { Uri.parse(it) } ?: parentRef!!.treeUri
    val docRef = DocumentRef(treeUri, documentId)

    mbob.setMediaId(docRef.mediaId)
    mbob._setMediaUri(mediaMeta, docRef.mediaUri)

    val size = getLong(DocumentsContract.Document.COLUMN_SIZE)
    if (size != null) {
        mediaMeta.size = size
    }

    val lastMod = getLong(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
    if (lastMod != null) {
        mediaMeta.lastModified = lastMod
    }

    val flags = getInt(DocumentsContract.Document.COLUMN_FLAGS)
    if (flags != null) {
        mediaMeta.documentFlags = flags
    }

    val dateAdded = getLong("date_added")
    if (dateAdded != null) {
        mediaMeta.dateAdded = dateAdded
    }

    val parentDocId = getString("parent_document_id")
    if (parentDocId != null) {
        mediaMeta.parentMediaId = DocumentRef(treeUri, parentDocId).mediaId
    } else {
        mediaMeta.parentMediaId = parentRef!!.mediaId
    }

    val title = getString("title")
    if (title != null) {
        mbob.setTitle(title)
    }

    val subtitle = getString("subtitle")
    if (subtitle != null) {
        mbob.setSubtitle(subtitle)
    }

    val artUriS = getString("artwork_uri")
    if (artUriS != null) {
        mediaMeta.artworkUri = Uri.parse(artUriS)
        mbob.setIconUri(Uri.parse(artUriS))
    }

    if (mediaMeta.isAudio) {
        val albumName = getString("album_name")
        if (albumName != null) {
            mediaMeta.albumName = albumName
        }
        val artistName = getString("artist_name")
        if (artistName != null) {
            mediaMeta.artistName = artistName
        }
        val albumArtistName = getString("album_artist_name")
        if (albumArtistName != null) {
            mediaMeta.albumArtistName = albumArtistName
        }
        val genreName = getString("genre_name")
        if (genreName != null) {
            mediaMeta.genreName = genreName
        }
        val releaseYear = getInt("release_year")
        if (releaseYear != null) {
            mediaMeta.releaseYear = releaseYear
        }
        val bitrate = getLong("bitrate")
        if (bitrate != null) {
            mediaMeta.bitrate = bitrate
        }
        val duration = getLong("duration")
        if (duration != null) {
            mediaMeta.duration = duration
        }
        val compilation = getInt("compilation")
        if (compilation != null && compilation != 0) {
            mediaMeta.isCompilation = true
        }
        val trackNum = getInt("track_number")
        if (trackNum != null) {
            mediaMeta.trackNumber = trackNum
        }
        val discNum = getInt("disc_number")
        if (discNum != null) {
            mediaMeta.discNumber = discNum
        }
        val artistUriS = getString("backdrop_uri")
        if (artistUriS != null) {
            mediaMeta.backdropUri = Uri.parse(artistUriS)
        }
    }

    return newMediaItem(mbob, mediaMeta)
}

const val MEDIA_DOCUMENT_SEL = "document_id = ? AND tree_uri = ?"
const val MEDIA_DOCUMENT_PARENT_SEL = "parent_document_id = ? AND tree_uri = ?"
fun mediaDocSelArgs(docRef: DocumentRef) = arrayOf(docRef.documentId, docRef.treeUri.toString())

/**
 *
 */
class NoSuchDocumentException : Exception()

/**
 *
 */
class InvalidMediaException: Exception()

/**
 *
 */
@ProviderScope
@dagger.Component(
        dependencies = arrayOf(
                AppContextComponent::class
        ),
        modules = arrayOf(
                MusicAuthorityModule::class
        )
)
interface MusicProviderComponent {
    fun inject(dbProvider: MusicDbProvider)
}

/**
 *
 */
@ProviderScope
class Database
@Inject
constructor(
        @ForApplication context: Context
) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        onUpgrade(db, 0, DATABASE_VERSION)
    }

    override fun onUpgrade(database: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val db = database!!
        if (oldVersion < DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS media_documents;")
            db.execSQL("CREATE TABLE media_documents (" +
                    "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "document_id TEXT NOT NULL," +
                    "_display_name TEXT NOT NULL," +
                    "mime_type TEXT," +
                    "last_modified INTEGER DEFAULT -1," +
                    "flags INTEGER DEFAULT 0," +
                    "_size INTEGER DEFAULT -1," +

                    "authority TEXT NOT NULL," +
                    "parent_document_id TEXT NOT NULL," +
                    "tree_uri TEXT NOT NULL," +
                    "date_added INTEGER," +
                    "title TEXT," +
                    "subtitle TEXT," +

                    "album_name TEXT," +
                    "artist_name TEXT," +
                    "album_artist_name TEXT," +
                    "genre_name TEXT," +
                    "release_year INTEGER," +
                    "bitrate INTEGER DEFAULT 0," +
                    "duration INTEGER DEFAULT 0," +
                    "compilation INTEGER DEFAULT 0," +
                    "track_number INTEGER DEFAULT 1," +
                    "disc_number INTEGER DEFAULT 1, " +
                    "artwork_uri TEXT," +
                    "backdrop_uri TEXT," +
                    "headers TEXT," +
                    //we will treat the same document under different trees
                    //as different documents
                    "UNIQUE(tree_uri, document_id)" +
                    ");")
        }
    }
}

