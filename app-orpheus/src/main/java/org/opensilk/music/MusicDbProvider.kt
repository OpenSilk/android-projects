package org.opensilk.music

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import org.opensilk.common.dagger.AppContextComponent
import org.opensilk.common.dagger.AppContextModule
import org.opensilk.common.dagger.NoDaggerComponentException
import org.opensilk.common.dagger.getDaggerComponent
import timber.log.Timber
import javax.inject.Inject

/**
 *
 */
class MusicDbProvider : ContentProvider() {

    @Inject internal lateinit var mDatabase: Database
    @Inject internal lateinit var mUris: MusicDbUris

    override fun onCreate(): Boolean {
        var rootCmp: AppContextComponent
        try {
            rootCmp = context.applicationContext.getDaggerComponent()
        } catch (e: NoDaggerComponentException) {
            Timber.i("No AppContextComponent found. Making our own")
            rootCmp = DaggerAppContextComponent.builder()
                    .appContextModule(AppContextModule(context!!.applicationContext))
                    .build()
        }
        val cmp = DaggerMusicProviderComponent.builder()
                .appContextComponent(rootCmp).build()
        cmp.inject(this)
        return true
    }

    override fun query(uri: Uri?, projection: Array<out String>?, selection: String?,
                       selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val db = mDatabase.readableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            MusicDbUris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.query(tbl, projection, selection, selectionArgs, null, null, sortOrder)
    }

    override fun insert(uri: Uri?, values: ContentValues?): Uri? {
        val db = mDatabase.writableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            MusicDbUris.MATCH_MEDIA_DOC -> tbl = "media_documents"
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
            MusicDbUris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.update(tbl, values, selection, selectionArgs)
    }

    override fun delete(uri: Uri?, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = mDatabase.writableDatabase ?: throw AssertionError("Unable to open database")
        val tbl: String
        when (mUris.matcher.match(uri)) {
            MusicDbUris.MATCH_MEDIA_DOC -> tbl = "media_documents"
            else -> throw IllegalArgumentException("unknown uri $uri")
        }
        return db.delete(tbl, selection, selectionArgs)
    }

    override fun getType(uri: Uri?): String? {
        throw UnsupportedOperationException()
    }
}