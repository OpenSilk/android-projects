package org.opensilk.autumn

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.LifecycleRegistryOwner
import android.arch.lifecycle.ServiceLifecycleDispatcher
import android.service.dreams.DreamService
import org.opensilk.autumn.databinding.DaydreamBinding

/**
 * Created by drew on 8/4/17.
 */
class DayDream : DreamService(), LifecycleRegistryOwner {

    private val mLifecycleRegistry = LifecycleRegistry(this)
    private val mLifeCycleDispacher = ServiceLifecycleDispatcher(this)

    private lateinit var mBinding: DaydreamBinding
    private lateinit var mViewModel: DreamViewModel

    override fun onAttachedToWindow() {
        mLifeCycleDispacher.onServicePreSuperOnCreate()
        super.onAttachedToWindow()
        setContentView(R.layout.daydream)
        mBinding = DaydreamBinding.bind(findViewById(R.id.dream_root))
        mViewModel = getApp().create(DreamViewModel::class.java)

        mViewModel.aspectRatio.observe(this, LiveDataObserver {
            mBinding.surfaceContainer.setAspectRatio(it)
        })
        mViewModel.loading.observe(this, LiveDataObserver {
            mBinding.isLoading = it
        })

        mViewModel.setSurface(mBinding.videoSurface)
    }

    override fun onDreamingStarted() {
        mLifeCycleDispacher.onServicePreSuperOnStart()
        super.onDreamingStarted()
        mViewModel.play()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        mViewModel.pause()
    }

    override fun onDetachedFromWindow() {
        mLifeCycleDispacher.onServicePreSuperOnDestroy()
        super.onDetachedFromWindow()
        mBinding.unbind()
        mViewModel.releaseSurface(mBinding.videoSurface)
        mViewModel.onCleared()
    }

    fun getApp(): App {
        return applicationContext as App
    }

    override fun getLifecycle(): LifecycleRegistry {
        return mLifecycleRegistry
    }

}