package org.opensilk.video

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by drew on 7/31/17.
 */
@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class OnceTest {

    @Test
    fun runsOnce() {
        val counter = AtomicInteger(0)
        val once = Once()
        once.Do {
            counter.incrementAndGet()
        }
        assertThat(counter.get()).isEqualTo(1)
        once.Do {
            counter.incrementAndGet()
        }
        assertThat(counter.get()).isEqualTo(1)
    }

    @Test
    fun runsOnce_multipleThreads() {
        val counter = AtomicInteger(0)
        val once = Once()
        val threads = ArrayList<Thread>()
        for (ii in 1..10) {
            val t = Thread({
                once.Do {
                    counter.incrementAndGet()
                }
            })
            t.start()
            threads.add(t)
        }
        for (ii in threads) {
            ii.join()
        }
        assertThat(counter.get()).isEqualTo(1)
    }
}