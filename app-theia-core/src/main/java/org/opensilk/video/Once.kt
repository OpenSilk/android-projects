package org.opensilk.video

import java.util.concurrent.atomic.AtomicBoolean

/**
 * A sync.Once ripoff from golang
 *
 * This implementation is not as safe since it does not block until Do has completed
 *
 * Created by drew on 7/31/17.
 */
class Once {
    private val done = AtomicBoolean(false)
    fun Do(f: () -> Unit) {
        if (done.compareAndSet(false, true)) {
            f()
        }
    }
}