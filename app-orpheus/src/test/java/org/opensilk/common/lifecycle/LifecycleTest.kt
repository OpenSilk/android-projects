package org.opensilk.common.lifecycle

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.opensilk.music.BuildConfig
import org.robolectric.annotation.Config
import rx.Subscriber
import rx.lang.kotlin.observable
import java.util.*
import org.assertj.core.api.Assertions.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by drew on 8/4/16.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21))
class LifecycleTest {

    lateinit var mLifecycle: LifecycleService

    @Before
    fun setup() {
        mLifecycle = LifecycleService()
    }

    @Test
    fun test_pauseAndResumeBinding() {
        val sub = object : Subscriber<Int>() {
            var nextVar = 0
            override fun onNext(t: Int?) {
                nextVar = t!!
            }
            var error: Throwable? = null
            override fun onError(e: Throwable?) {
                error = e!!
            }
            var completeCalled = false
            override fun onCompleted() {
                completeCalled = true
            }
        }
        val atomicInt = AtomicInteger(1)
        observable<Int> { subscriber ->
            subscriber.onNext(atomicInt.getAndIncrement())
        }.connectedToPauseResume(mLifecycle).subscribe(sub)
        mLifecycle.onResume()
        assertThat(sub.nextVar).isEqualTo(1)
        mLifecycle.onPause()
        assertThat(sub.completeCalled).isFalse()
        mLifecycle.onResume()
        assertThat(sub.nextVar).isEqualTo(2)
        mLifecycle.onDestroy()
        assertThat(sub.completeCalled).isTrue()
    }

    @Test
    fun test_bind() {
        val sub = object : Subscriber<Int>() {
            var nextVar = 0
            override fun onNext(t: Int?) {
                nextVar = t!!
            }
            var error: Throwable? = null
            override fun onError(e: Throwable?) {
                error = e!!
            }
            var completeCalled = false
            override fun onCompleted() {
                completeCalled = true
            }
        }
        val atomicInt = AtomicInteger(1)
        mLifecycle.onCreate()
        observable<Int> { s ->
            s.onNext(atomicInt.andIncrement)
        }.compose<Int>(mLifecycle.bind()).subscribe(sub)
        //subscribe will publish
        assertThat(sub.nextVar).isEqualTo(1)
        mLifecycle.onStart()
        mLifecycle.onStop()
        //above events should be ignored
        assertThat(sub.completeCalled).isFalse()
        mLifecycle.onDestroy()
        //destroy should trigger completed
        assertThat(sub.completeCalled).isTrue()

    }

}