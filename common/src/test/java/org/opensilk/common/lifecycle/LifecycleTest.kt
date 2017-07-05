package org.opensilk.common.lifecycle

import android.content.Context
import com.nhaarman.mockito_kotlin.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import rx.Subscriber
import rx.lang.kotlin.observable
import org.assertj.core.api.Assertions.*
import org.mockito.Mockito
import org.opensilk.common.BuildConfig
import org.robolectric.RobolectricTestRunner
import rx.Observer
import rx.Subscription
import rx.lang.kotlin.single
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by drew on 8/4/16.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21))
class LifecycleTest {

    lateinit var mLifecycle: LifecycleService
    lateinit var mContext: Context
    lateinit var mSub: Observer<Int>

    @Before
    fun setup() {
        mLifecycle = LifecycleService()
        mContext = mock()
        whenever(mContext.getSystemService(LIFECYCLE_SERVICE)).thenReturn(mLifecycle)
        mSub = mock()
    }

    @Test
    fun test_single_terminate_on_destroy() {
        mLifecycle.onCreate()
        val s = single<Int> { }.terminateOnDestroy(mContext).subscribe(mSub)
        mLifecycle.onDestroy()
        assertThat(s.isUnsubscribed).isTrue()
        verify(mSub, never()).onNext(0)
        //cancel triggers exception
        verify(mSub).onError(Mockito.any(CancellationException::class.java))
    }

    @Test
    fun test_pauseAndResumeBinding() {
        mLifecycle.onCreate()
        var s = observable<Int> { it.onNext(0) }.connectedToPauseResume(mContext).subscribe(mSub)
        mLifecycle.onResume()
        verify(mSub).onNext(0)
        mLifecycle.onPause()
        verify(mSub, never()).onCompleted()
        mLifecycle.onResume()
        verify(mSub, times(2)).onNext(0)
        mLifecycle.onPause()
        mLifecycle.onDestroy()
        verify(mSub).onCompleted()
    }

    @Test
    fun test_bind() {
        mLifecycle.onCreate()
        val s = observable<Int> { it.onNext(0) }.compose<Int>(mLifecycle.bind()).subscribe(mSub)
        //subscribe will publish
        verify(mSub).onNext(0)
        mLifecycle.onStart()
        mLifecycle.onStop()
        //above events should be ignored
        assertThat(s.isUnsubscribed).isFalse()
        mLifecycle.onDestroy()
        //destroy should trigger completed
        verify(mSub).onCompleted()
    }

}