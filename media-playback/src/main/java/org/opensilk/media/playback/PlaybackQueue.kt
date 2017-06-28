package org.opensilk.media.playback

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.media.session.MediaSession.*
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

/**
 * Created by drew on 2/24/17.
 */
class PlaybackQueue
@Inject
constructor() {

    private val mIdGen = AtomicLong(1)
    private val mQueue = LinkedList<QueueItem>()
    private val mHistory = LinkedList<Long>()
    private var mCurrent: QueueItem = EMPTY_ITEM

    companion object {
        val EMPTY_DESCRIPTION: MediaDescription = MediaDescription.Builder().build()
        val EMPTY_ITEM: QueueItem = QueueItem(EMPTY_DESCRIPTION, 0)
    }

    fun getCurrent(): rx.Single<QueueItem> {
        return rx.Single.create { s ->
            if (mCurrent == EMPTY_ITEM) {
                s.onError(NoSuchElementException())
                return@create
            }
            s.onSuccess(mCurrent)
        }
    }

    fun getNext(): rx.Single<QueueItem> {
        return rx.Single.create { s ->
            var idx = mQueue.indexOf(mCurrent)
            if (idx < 0) {
                s.onError(NoSuchElementException())
                return@create
            }
            idx += 1
            if (idx >= mQueue.size) {
                idx = 0
            }
            val nxt = mQueue[idx]
            s.onSuccess(nxt)
        }
    }

    fun goToPrevious(): rx.Single<QueueItem> {
        return rx.Single.create { s ->
            if (mHistory.isEmpty()) {
                s.onError(NoSuchElementException())
                return@create
            }
            val id = mHistory.removeLast()
            val nxt = mQueue.firstOrNull { it.queueId == id }
            if (nxt == null) {
                s.onError(NoSuchElementException())
                return@create
            }
            mCurrent = nxt
            s.onSuccess(nxt)
        }
    }

    fun goToNext(): rx.Single<QueueItem> {
        return rx.Single.create { s ->
            var idx = mQueue.indexOf(mCurrent)
            if (idx < 0) {
                s.onError(NoSuchElementException())
                return@create
            }
            idx += 1
            if (idx >= mQueue.size) {
                idx = 0
            }
            val nxt = mQueue[idx]
            if (mCurrent.queueId > 0) {
                mHistory.add(mCurrent.queueId)
            }
            mCurrent = nxt
            s.onSuccess(nxt)
        }
    }

    fun goToItem(itemId: Long): rx.Single<QueueItem> {
        return rx.Single.create { s ->
            val nxt = mQueue.firstOrNull { it.queueId == itemId }
            if (nxt == null) {
                s.onError(NoSuchElementException())
                return@create
            }
            val idx = mQueue.indexOf(nxt)
            if (idx < 0) {
                s.onError(NoSuchElementException())
                return@create
            }
            if (mCurrent.queueId > 0) {
                mHistory.add(mCurrent.queueId)
            }
            mCurrent = mQueue[idx]
            s.onSuccess(mCurrent)
        }
    }

    fun newItem(mediaDescription: MediaDescription): QueueItem {
        return QueueItem(mediaDescription, mIdGen.getAndIncrement())
    }

    fun add(item: QueueItem) : Boolean {
        val success = mQueue.add(item)
        if (success && mQueue.size == 1) {
            mCurrent = item
        }
        return success
    }

    fun add(desc: MediaDescription) : Boolean {
        return add(newItem(desc))
    }

    fun remove(itemId: Long) : Boolean {
        var idx = mQueue.indexOf(mCurrent)
        val success = mQueue.removeIf { it.queueId == itemId }
        if (success && mCurrent.queueId == itemId) {
            if (idx < 0 || idx >= mQueue.size) {
                idx = 0 //reset if last item
            }
            mCurrent = mQueue[idx]
        }
        return success
    }

    fun clear() {
        mQueue.clear()
        mHistory.clear()
        mCurrent = EMPTY_ITEM
    }

    fun get(): List<QueueItem> {
        return ArrayList(mQueue)
    }
}