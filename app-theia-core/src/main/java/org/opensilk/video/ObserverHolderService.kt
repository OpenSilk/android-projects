package org.opensilk.video

import android.app.Service
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.ProcessLifecycleOwner
import android.content.Intent
import android.os.IBinder
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

@Module
abstract class ObserverHolderServiceModule {
    @ContributesAndroidInjector
    abstract fun holderService(): ObserverHolderService
}

/**
 * Service that holds observers of the process lifecycle
 */
class ObserverHolderService : Service() {

    @Inject lateinit var mUpnpDevicesObserver: UpnpDevicesObserver
    @Inject lateinit var mStorageDevicesObserver: StorageDevicesObserver

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        lifecycle.addObserver(mUpnpDevicesObserver)
        lifecycle.addObserver(mStorageDevicesObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val lifecycle: Lifecycle by lazy {
        ProcessLifecycleOwner.get().lifecycle
    }
}