package org.opensilk.video

import android.app.Application
import android.content.ContentProvider
import dagger.android.AndroidInjector
import dagger.android.HasContentProviderInjector
import org.opensilk.logging.DebugTreeWithThreadName
import timber.log.Timber

/**
 * App that does not do the injection
 *
 * Created by drew on 8/6/17.
 */
class TestApp: Application(), HasContentProviderInjector {
    override fun onCreate() {
        super.onCreate()
        Timber.uprootAll()
        Timber.plant(DebugTreeWithThreadName)
    }
    override fun contentProviderInjector(): AndroidInjector<ContentProvider> =
            AndroidInjector { /*pass*/ }
}