package org.opensilk.video

import android.app.Application
import org.opensilk.common.dagger.InjectionManager

/**
 * App that does not do the injection
 *
 * Created by drew on 8/6/17.
 */
class TestApp: Application(), InjectionManager {
    override fun injectFoo(foo: Any): Any {
        return ""
    }
}