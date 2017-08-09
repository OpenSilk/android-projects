package org.opensilk.media.playback

import android.media.MediaDescription
import android.media.browse.MediaBrowser
import android.media.session.MediaSession.*
import io.reactivex.Maybe
import io.reactivex.Single
import org.opensilk.media.toMediaItem
import timber.log.Timber
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList

class NoCurrentItemException: Exception("Queue is not initialized")

/**
 * Created by drew on 2/24/17.
 */
class PlaybackQueue
@Inject
constructor() {

    private val mIdGen = AtomicLong(1)
    private val mQueue = LinkedList<QueueItem>()
    private val mHistory = LinkedList<Long>()
    private var mCurrent = -1
    private var mWrap = true

    fun setWrap(wrap: Boolean) {
        mWrap = wrap
    }

    fun getCurrent(): Single<QueueItem> {
        return Single.create { s ->
            Timber.d("getCurrent()")
            if (mCurrent < 0) {
                s.onError(NoCurrentItemException())
                return@create
            }
            s.onSuccess(mQueue[mCurrent])
        }
    }

    fun getNext(): Maybe<QueueItem> {
        return Maybe.create { s ->
            Timber.d("getNext()")
            if (mCurrent < 0) {
                s.onError(NoCurrentItemException())
                return@create
            }
            var idx = mCurrent + 1
            if (idx >= mQueue.size) {
                if (!mWrap) {
                    s.onComplete()
                    return@create
                }
                idx = 0
            }
            val nxt = mQueue[idx]
            s.onSuccess(nxt)
        }
    }

    fun hasNext(): Boolean {
        if (mCurrent < 0) {
            return false
        }
        return (mCurrent + 1) < mQueue.size || mWrap
    }

    fun goToPrevious(): Maybe<QueueItem> {
        return Maybe.create { s ->
            Timber.d("goToPrevious()")
            if (mHistory.isEmpty()) {
                s.onComplete()
                return@create
            }
            val id = mHistory.removeLast()
            val nxt = mQueue.indexOfLast { it.queueId == id }
            if (nxt < 0) {
                s.onError(NoSuchElementException())
                return@create
            }
            mCurrent = nxt
            s.onSuccess(mQueue[nxt])
        }
    }

    fun hasPrevious(): Boolean {
        if (mHistory.isEmpty()) {
            return false
        }
        return mQueue.indexOfLast { mHistory.peekLast() == it.queueId } != -1
    }

    fun goToNext(): Maybe<QueueItem> {
        return Maybe.create { s ->
            Timber.d("goToNext()")
            if (mCurrent < 0) {
                s.onError(NoCurrentItemException())
                return@create
            }
            var idx = mCurrent + 1
            if (idx >= mQueue.size) {
                if (!mWrap) {
                    s.onComplete()
                    return@create
                }
                idx = 0
            }
            mHistory.add(mQueue[mCurrent].queueId)
            mCurrent = idx
            s.onSuccess(mQueue[mCurrent])
        }
    }

    fun setCurrent(itemId: Long) {
        Timber.d("setCurrent()")
        val nxt = mQueue.indexOfFirst { it.queueId == itemId }
        if (nxt < 0) {
            throw NoSuchElementException()
        }
        if (mCurrent >= 0 && mCurrent < mQueue.size) {
            mHistory.add(mQueue[mCurrent].queueId)
        }
        mCurrent = nxt
    }

    fun newItem(mediaDescription: MediaDescription): QueueItem {
        return QueueItem(mediaDescription, mIdGen.getAndIncrement())
    }

    fun add(item: QueueItem) : Boolean {
        val success = mQueue.add(item)
        if (success && mQueue.size == 1) {
            mCurrent = 0
        }
        return success
    }

    fun add(desc: MediaDescription) : Boolean {
        return add(newItem(desc))
    }

    fun remove(itemId: Long) : Boolean {
        val idx = mQueue.indexOfFirst { it.queueId == itemId }
        if (idx < 0) {
            return false
        }
        mQueue.removeAt(idx)
        if (mCurrent >= mQueue.size) {
            if (mWrap) {
                mCurrent = 0
            } else {
                mCurrent = -1
            }
        }
        return true
    }

    fun clear() {
        mQueue.clear()
        mHistory.clear()
        mCurrent = -1
    }

    fun get(): List<QueueItem> {
        return ArrayList(mQueue)
    }

    fun notEmpty(): Boolean {
        return !mQueue.isEmpty()
    }
}