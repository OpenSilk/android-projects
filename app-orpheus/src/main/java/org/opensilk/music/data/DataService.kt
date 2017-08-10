package org.opensilk.music.data

import android.content.Context
import android.database.ContentObserver
import android.media.browse.MediaBrowser
import android.net.Uri
import android.provider.DocumentsContract
import org.opensilk.common.dagger.ForApplication
import org.opensilk.media._getMediaMeta
import org.opensilk.media._getMediaTitle
import org.opensilk.media._newBuilder
import org.opensilk.media.newMediaItem
import org.opensilk.music.*
import org.opensilk.music.data.ref.DocumentRef
import org.opensilk.music.data.ref.MediaRef
import rx.Observable
import rx.Scheduler
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.observable
import rx.lang.kotlin.single
import rx.lang.kotlin.subscriber
import rx.schedulers.Schedulers
import rx.subscriptions.Subscriptions
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by drew on 6/26/16.
 */
@Singleton
class DataService
@Inject
constructor(
        @ForApplication private val mContext: Context,
        private val mDBClient: MusicDbClient,
        private val mUris: MusicDbUris

    ) {

    companion object {
        val sSubscribeScheduler: Scheduler = Schedulers.io()
        val sMainScheduler: Scheduler = AndroidSchedulers.mainThread()
    }

    fun subscribeChanges(mediaItem: MediaBrowser.MediaItem, emmitInitial: Boolean = true): Observable<Boolean> {
        val docRef = mediaItem._getMediaRef()
        return if (docRef is DocumentRef) {
            subscribeChanges(docRef, emmitInitial = emmitInitial)
        } else {
            Observable.error(IllegalArgumentException("Unsupported mediaRef"))
        }
    }

    fun subscribeChanges(docRef: DocumentRef? = null, emmitInitial: Boolean = true): Observable<Boolean> {
        val subUri = mUris.mediaDocs()
        val o: Observable<Boolean> = observable { subscriber ->
            val co = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    subscriber.onNext(true)
                }
            }
            subscriber.add(Subscriptions.create {
                mContext.contentResolver.unregisterContentObserver(co)
            })
            mContext.contentResolver.registerContentObserver(subUri, true, co)
        }
        return if (emmitInitial) o.startWith(true) else o
    }

    fun notifyItemChanged(mediaItem: MediaBrowser.MediaItem) {
        mContext.contentResolver.notifyChange(mUris.mediaDocs(), null)
    }

    fun insertRoot(treeUri: Uri): rx.Single<Boolean> {
        return single<Boolean> { subscriber ->
            val docRef = DocumentRef.root(treeUri)
            val success = mDBClient.insertRootDoc(docRef)
            if (success) {
                mContext.contentResolver.notifyChange(mUris.mediaDocs(), null)
            }
            subscriber.onSuccess(success)
        }
    }

    fun removeRoot(mediaItem: MediaBrowser.MediaItem): rx.Single<Boolean> {
        return single<Boolean> { subscriber ->
            val mediaRef = MediaRef.parse(mediaItem.mediaId)
            val success = when (mediaRef) {
                is DocumentRef -> mDBClient.removeRootDoc(mediaRef)
                else -> false
            }
            subscriber.onSuccess(success)
        }
    }

    fun getRoots(): rx.Single<List<MediaBrowser.MediaItem>> {
        return Single.just(mDBClient.getRootDocs())
    }

    fun getDocChildren(docRef: DocumentRef): rx.Single<List<MediaBrowser.MediaItem>> {
        return single<List<MediaBrowser.MediaItem>> { subscriber ->
            mContext.contentResolver.query(docRef.childrenUri, DOCUMENT_COLS,
                    null, null, DocumentsContract.Document.COLUMN_DISPLAY_NAME)?.use { c ->
                if (c.moveToFirst()) {
                    val colMap = c.mapCols(DOCUMENT_COLS)
                    val list = ArrayList<MediaBrowser.MediaItem>(c.count)
                    do {
                        list.add(c.makeDocMediaItem(colMap, docRef))
                    } while (c.moveToNext())
                    subscriber.onSuccess(list)
                } else {
                    subscriber.onSuccess(emptyList())
                }
            } ?: subscriber.onError(NullPointerException("Unable to obtain cursor"))
        }.zipWith(Single.just(mDBClient.getDocChildren(docRef))) { pulled, cached ->
            val combined = ArrayList<MediaBrowser.MediaItem>(pulled.size)
            for (pulledMedia in pulled) {
                val cachedMedia = cached.find { it.mediaId == pulledMedia.mediaId }
                if (cachedMedia != null) {
                    //reconcile cached data with things that might have changed
                    val bob = cachedMedia.description._newBuilder()
                    val mom = cachedMedia.description._getMediaMeta()
                    val omg = pulledMedia._getMediaMeta()
                    if (pulledMedia.description.title?.isNotBlank() ?: false) {
                        bob.setTitle(pulledMedia.description.title)
                    }
                    if (pulledMedia.description.subtitle?.isNotBlank() ?: false) {
                        bob.setSubtitle(pulledMedia.description.subtitle)
                    }
                    if (omg.displayName.isNotBlank()) {
                        mom.displayName = omg.displayName
                    }
                    if (omg.size > 0) {
                        mom.size = omg.size
                    }
                    if (omg.lastModified > 0) {
                        mom.lastModified = omg.lastModified
                    }
                    if (omg.documentFlags != mom.documentFlags) {
                        mom.documentFlags = omg.documentFlags
                    }
                    combined.add(newMediaItem(bob, mom))
                } else {
                    combined.add(pulledMedia)
                }
            }
            return@zipWith combined as List<MediaBrowser.MediaItem>
        }.doOnSuccess { items ->
            for (item in items) {
                mDBClient.insertMediaDoc(item)
            }
            val itemRefs = items.filter {
                it._likelyDocument() //ensure cast doesn't fail
            }.map {
                it._getMediaRef() as DocumentRef
            }
            mDBClient.removeOrphanDocs(docRef, itemRefs)
        }
    }

}