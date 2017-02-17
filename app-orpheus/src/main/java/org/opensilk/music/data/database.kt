package org.opensilk.music.data

import android.content.*
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import org.opensilk.common.dagger2.*
import org.opensilk.common.dagger2.AppContextComponent
import org.opensilk.common.dagger2.AppContextModule
import org.opensilk.common.dagger2.ForApplication
import org.opensilk.common.dagger2.ProviderScope
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MediaRef
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named

/**
 * Created by drew on 6/26/16.
 */
const val DATABASE_NAME = "music.sqlite"
const val DATABASE_VERSION = 3

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
        "artist_artwork_uri",
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

    val mediaMeta = MediaMeta.empty()
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
        val artistUriS = getString("artist_artwork_uri")
        if (artistUriS != null) {
            mediaMeta.artistArtworkUri = Uri.parse(artistUriS)
        }
    }

    return newMediaItem(mbob, mediaMeta)
}

const val MEDIA_DOCUMENT_SEL = "document_id = ? AND tree_uri = ?"
const val MEDIA_DOCUMENT_PARENT_SEL = "parent_document_id = ? AND tree_uri = ?"
fun mediaDocSelArgs(docRef: DocumentRef) = arrayOf(docRef.documentId, docRef.treeUri.toString())

class MusicProvider() : ContentProvider() {

    @ProviderScope
    @dagger.Component(
            dependencies = arrayOf(
                    AppContextComponent::class
            ),
            modules = arrayOf(
                    MusicAuthorityModule::class
            )
    )
    interface Component {
        fun inject(provider: MusicProvider)
    }

    class Client
    @Inject
    constructor(
            @ForApplication private val mContext: Context,
            private val mUris: Uris
    ) {

        fun removeOrphanDocs(parentRef: DocumentRef, currentChildren: List<DocumentRef>) {
            mContext.contentResolver.query(mUris.mediaDocs(),
                    arrayOf("tree_uri", "document_id"),
                    "parent_document_id = ? AND tree_uri = ?",
                    arrayOf(parentRef.documentId, parentRef.treeUri.toString()), null).use { c ->
                if (c.moveToFirst()) {
                    do {
                        val childRef = DocumentRef(Uri.parse(c.getString(0)), c.getString(1))
                        if (childRef !in currentChildren) {
                            removeDoc(childRef)
                        }
                    } while (c.moveToNext())
                }
            }
        }

        private fun removeDoc(docRef: DocumentRef): Int {
            val isDir = mContext.contentResolver.query(mUris.mediaDocs(), arrayOf("mime_type"),
            MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
                if (c.moveToFirst()) {
                    val mime = c.getString(0)
                    return@use mime == DocumentsContract.Document.MIME_TYPE_DIR
                } else {
                    return@use false
                }
            }
            var num = 0
            if (isDir) {
                val children: List<DocumentRef> = mContext.contentResolver.query(
                        mUris.mediaDocs(), arrayOf("tree_uri", "document_id"),
                        MEDIA_DOCUMENT_PARENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
                    return@use if (c.moveToFirst()) {
                        ArrayList<DocumentRef>(c.count).apply {
                            do {
                                add(DocumentRef(Uri.parse(c.getString(0)), c.getString(1)))
                            } while (c.moveToNext())
                        }
                    } else {
                        emptyList<DocumentRef>()
                    }
                }
                children.forEach {
                    num += removeDoc(it)
                }
            }
            num += mContext.contentResolver.delete(mUris.mediaDocs(),
                    MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef))
            return num
        }

        fun insertRootDoc(rootRef: DocumentRef): Boolean {
            return mContext.contentResolver.query(rootRef.mediaUri,
                    DOCUMENT_COLS, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(DOCUMENT_COLS)
                    val parentRootRef = rootRef.copy(documentId = ROOTS_PARENT)
                    val rootMedia = c.makeDocMediaItem(colMap, parentRootRef)
                    val rootMeta = rootMedia._getMediaMeta()
                    val returnedRef = rootMedia._getMediaRef()
                    //make sure they sent us the document we requested
                    val isSameDocument = returnedRef.mediaId == rootRef.mediaId
                    if (isSameDocument && rootMeta.isDirectory) {
                        return@use insertMediaDoc(rootMedia)
                    } else {
                        return@use false
                    }
                } else {
                    return@use false
                }
            } ?: false
        }

        fun removeRootDoc(rootRef: DocumentRef): Boolean {
            return removeDoc(rootRef) > 0
        }

        fun getRootDocs(): List<MediaBrowser.MediaItem> {
            return mContext.contentResolver.query(mUris.mediaDocs(), ROOT_COLS,
                    "parent_document_id = ?", arrayOf(ROOTS_PARENT),
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME).use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(ROOT_COLS)
                    val lst = ArrayList<MediaBrowser.MediaItem>(c.count)
                    do {
                        lst.add(c.makeDocMediaItem(colMap))
                    } while (c.moveToNext())
                    return@use lst
                } else {
                    return@use emptyList()
                }
            }
        }

        fun insertMediaDoc(mediaItem: MediaBrowser.MediaItem): Boolean {
            val mediaMeta = mediaItem._getMediaMeta()
            val cv = ContentValues()

            val docRef = mediaItem._getMediaRef()
            if (docRef !is DocumentRef) {
                Timber.w("Media %s is not a document", mediaMeta.displayName)
                return false
            }

            val parentRef = mediaMeta.parentRef
            if (parentRef !is DocumentRef) {
                Timber.w("Media %s does not have a valid parent id=%s",
                        mediaMeta.displayName, mediaMeta.parentMediaId)
                return false
            }

            cv.put(DocumentsContract.Document.COLUMN_DISPLAY_NAME, mediaMeta.displayName)
            cv.put(DocumentsContract.Document.COLUMN_MIME_TYPE, mediaMeta.mimeType)
            cv.put(DocumentsContract.Document.COLUMN_SIZE, mediaMeta.size)
            cv.put(DocumentsContract.Document.COLUMN_LAST_MODIFIED, mediaMeta.lastModified)
            cv.put(DocumentsContract.Document.COLUMN_FLAGS, mediaMeta.documentFlags)

            cv.put("authority", docRef.authority)
            cv.put("parent_document_id", parentRef.documentId)
            cv.put("title", mediaItem.description.title?.toString() ?: mediaMeta.displayName)
            cv.put("subtitle", mediaItem.description.subtitle?.toString())

            if (mediaMeta.isAudio) {
                cv.put("album_name", mediaMeta.albumName)
                cv.put("artist_name", mediaMeta.artistName)
                cv.put("album_artist_name", mediaMeta.genreName)
                cv.put("genre_name", mediaMeta.genreName)
                cv.put("release_year", mediaMeta.releaseYear)
                cv.put("bitrate", mediaMeta.bitrate)
                cv.put("duration", mediaMeta.duration)
                cv.put("compilation", if (mediaMeta.isCompilation) 1 else 0)
                cv.put("track_number", mediaMeta.trackNumber)
                cv.put("disc_number", mediaMeta.discNumber)
                if (mediaMeta.artworkUri != Uri.EMPTY) {
                    cv.put("artwork_uri", mediaMeta.artworkUri.toString())
                }
                if (mediaMeta.artistArtworkUri != Uri.EMPTY) {
                    cv.put("artist_artwork_uri", mediaMeta.artistArtworkUri.toString())
                }
                cv.put("headers", mediaMeta.mediaHeaders)
            }

            val num = mContext.contentResolver.update(mUris.mediaDocs(), cv,
                    MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef))
            if (num > 0) {
                Timber.d("Updated media %s", mediaMeta.displayName)
                return true
            }
            cv.put(DocumentsContract.Document.COLUMN_DOCUMENT_ID, docRef.documentId)
            cv.put("tree_uri", docRef.treeUri.toString())
            cv.put("date_added", System.currentTimeMillis())
            return mContext.contentResolver.insert(mUris.mediaDocs(), cv) != null
        }

        fun getMediaDoc(docRef: DocumentRef): MediaBrowser.MediaItem? {
            return mContext.contentResolver.query(mUris.mediaDocs(), MEDIA_COLS,
                    MEDIA_DOCUMENT_SEL, mediaDocSelArgs(docRef), null).use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(MEDIA_COLS)
                    return@use c.makeDocMediaItem(colMap)
                } else {
                    return@use null
                }
            }
        }

        fun getDocChildren(docRef: DocumentRef) : List<MediaBrowser.MediaItem> {
            return mContext.contentResolver.query(mUris.mediaDocs(), MEDIA_COLS,
                    "parent_document_id = ? AND tree_uri = ?",
                    arrayOf(docRef.documentId, docRef.treeUri.toString()),
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME).use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(MEDIA_COLS)
                    val lst = ArrayList<MediaBrowser.MediaItem>(c.count)
                    do {
                        lst.add(c.makeDocMediaItem(colMap))
                    } while (c.moveToNext())
                    return@use lst
                } else {
                    return@use emptyList()
                }
            }
        }



    }

    class Uris
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
                        "artist_artwork_uri TEXT," +
                        "headers TEXT," +
                //we will treat the same document under different trees
                //as different documents
                        "UNIQUE(tree_uri, document_id)" +
                        ");")
            }
        }
    }

    @Inject internal lateinit var mDatabase: Database
    @Inject internal lateinit var mUris: Uris

    override fun onCreate(): Boolean {
        var rootCmp: AppContextComponent
        try {
            rootCmp = getDaggerComponent(context!!.applicationContext)
        } catch (e: NoDaggerComponentException) {
            Timber.i("No AppContextComponent found. Making our own")
            rootCmp = DaggerAppContextComponent.builder()
                    .appContextModule(AppContextModule(context!!.applicationContext))
                    .build()
        }
        val cmp = DaggerMusicProvider_Component.builder()
                .appContextComponent(rootCmp).build()
        cmp.inject(this)
        return true
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val db = mDatabase.readableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            Uris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.query(tbl, projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri?, values: ContentValues?): Uri? {
        val db = mDatabase.writableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            Uris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        val row = db.insert(tbl, null, values)
        return ContentUris.withAppendedId(uri, row)
    }

    override fun update(uri: Uri?, values: ContentValues?, selection: String?,
                        selectionArgs: Array<out String>?): Int {
        val db = mDatabase.writableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            Uris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.update(tbl, values, selection, selectionArgs)
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = mDatabase.writableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            Uris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.delete(tbl, selection, selectionArgs)
    }

    override fun getType(uri: Uri?): String? {
        throw UnsupportedOperationException()
    }
}