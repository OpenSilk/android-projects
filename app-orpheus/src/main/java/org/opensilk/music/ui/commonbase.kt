package org.opensilk.music.ui

import android.arch.lifecycle.LifecycleFragment
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.support.v7.app.AppCompatActivity
import io.reactivex.Scheduler
import org.opensilk.music.data.AutoClearedValue

/**
 * Created by drew on 6/28/16.
 */
abstract class BaseSlidingActivity: AppCompatActivity(), LifecycleRegistryOwner {

    protected lateinit var mMainWorker: Scheduler.Worker

    override fun onDestroy() {
        super.onDestroy()
        mMainWorker.dispose()
    }

    private val myLifecycleRegistry: LifecycleRegistry by lazy { LifecycleRegistry(this) }
    override fun getLifecycle(): LifecycleRegistry = myLifecycleRegistry

}

abstract class BaseMusicFragment: LifecycleFragment(), AutoClearedValue.Host {
    private val autoValues = HashSet<AutoClearedValue<*, *>>()

    override fun registerAutoClearedValue(value: AutoClearedValue<*, *>) {
        autoValues.add(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoValues.forEach { it.clear() }
    }
}
