package org.opensilk.media.playback

import android.media.MediaDescription
import android.media.session.MediaSession.QueueItem
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber
import java.util.*
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
            s.onSuccess(mQueue[idx])
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
            var nxt = -1
            while (nxt < 0 && mHistory.isNotEmpty()) {
                val id = mHistory.removeLast()
                nxt = mQueue.indexOfLast { it.queueId == id }
            }
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
            maybeAddCurrentToHistory()
            var idx = mCurrent + 1
            if (idx >= mQueue.size) {
                if (!mWrap) {
                    s.onComplete()
                    return@create
                }
                idx = 0
            }
            mCurrent = idx
            s.onSuccess(mQueue[idx])
        }
    }

    fun setCurrent(itemId: Long) {
        Timber.d("setCurrent()")
        val nxt = mQueue.indexOfFirst { it.queueId == itemId }
        if (nxt < 0) {
            throw NoSuchElementException()
        }
        maybeAddCurrentToHistory()
        mCurrent = nxt
    }

    fun newItem(mediaDescription: MediaDescription): QueueItem =
            QueueItem(mediaDescription, mIdGen.getAndIncrement())

    fun add(item: QueueItem) : Boolean {
        val success = mQueue.add(item)
        if (success && mQueue.size == 1) {
            mCurrent = 0
        }
        return success
    }

    fun add(desc: MediaDescription) : Boolean = add(newItem(desc))

    fun remove(itemId: Long) : Boolean {
        val idx = mQueue.indexOfFirst { it.queueId == itemId }
        if (idx < 0) {
            return false
        }
        mQueue.removeAt(idx)
        if (mCurrent >= mQueue.size) {
            mCurrent = if (mWrap) 0 else -1
        }
        return true
    }

    fun clear() {
        mQueue.clear()
        mHistory.clear()
        mCurrent = -1
    }

    fun get(): List<QueueItem> = ArrayList(mQueue)

    fun notEmpty(): Boolean = !mQueue.isEmpty()

    private fun maybeAddCurrentToHistory() {
        if (mCurrent >= 0 && mCurrent < mQueue.size) {
            mHistory.add(mQueue[mCurrent].queueId)
        }
    }
}