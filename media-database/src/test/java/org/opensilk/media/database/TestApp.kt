package org.opensilk.media.database

import android.content.ContentProvider
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import dagger.android.DaggerApplication_MembersInjector

/**
 * App that does not do the injection
 *
 * Created by drew on 8/6/17.
 */
class TestApp: DaggerApplication() {
    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return AndroidInjector<TestApp> {
            DaggerApplication_MembersInjector.injectSetInjected(this)
        }
    }

    override fun contentProviderInjector(): AndroidInjector<ContentProvider> {
        return AndroidInjector {
            //pass
        }
    }
}