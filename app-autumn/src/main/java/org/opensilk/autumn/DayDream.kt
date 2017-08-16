package org.opensilk.autumn

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.os.Handler
import android.service.dreams.DreamService
import org.opensilk.autumn.databinding.DaydreamBinding

/**
 * Created by drew on 8/4/17.
 */
class DayDream : DreamService(), LifecycleRegistryOwner {

    private val mLifecycleRegistry = LifecycleRegistry(this)
    private val mLifecycleHandler = Handler()

    private lateinit var mBinding: DaydreamBinding
    private lateinit var mViewModel: DreamViewModel

    private var mLastDispatchRunnable: DispatchRunnable? = null

    override fun onCreate() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate()
    }

    override fun onDestroy() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onAttachedToWindow() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_START)
        super.onAttachedToWindow()
        setContentView(R.layout.daydream)
        mBinding = DaydreamBinding.bind(findViewById(R.id.dream_root))
        mViewModel = application.create(DreamViewModel::class.java)

        mViewModel.aspectRatio.observe(this, LiveDataObserver {
            mBinding.surfaceContainer.setAspectRatio(it)
        })
        mViewModel.loading.observe(this, LiveDataObserver {
            mBinding.isLoading = it
        })
        mViewModel.label.observe(this, LiveDataObserver {
            mBinding.labelText = it
        })

        mViewModel.setSurface(mBinding.videoSurface)
    }

    override fun onDreamingStarted() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_RESUME)
        super.onDreamingStarted()
        mViewModel.play()
    }

    override fun onDreamingStopped() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onDreamingStopped()
        mViewModel.pause()
    }

    override fun onDetachedFromWindow() {
        dispatchLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onDetachedFromWindow()
        mBinding.unbind()
        mViewModel.releaseSurface(mBinding.videoSurface)
        mViewModel.onCleared()
    }

    override fun getLifecycle(): LifecycleRegistry = mLifecycleRegistry

    private val application: App
            get() = applicationContext as App

    private fun dispatchLifecycleEvent(event: Lifecycle.Event) {
        mLastDispatchRunnable?.run()
        mLastDispatchRunnable = DispatchRunnable(this, event)
        mLifecycleHandler.postAtFrontOfQueue(mLastDispatchRunnable)
    }

    private class DispatchRunnable(
            private val mRegistry: LifecycleRegistryOwner,
            private val mEvent: Lifecycle.Event
    ) : Runnable {
        private var mWasExecuted = false

        override fun run() {
            if (!mWasExecuted) {
                mRegistry.lifecycle.handleLifecycleEvent(mEvent)
                mWasExecuted = true
            }
        }
    }
}