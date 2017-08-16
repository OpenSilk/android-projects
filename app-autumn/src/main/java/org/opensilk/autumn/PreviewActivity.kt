package org.opensilk.autumn

import android.arch.lifecycle.LifecycleActivity
import android.databinding.DataBindingUtil
import android.os.Bundle
import org.opensilk.autumn.databinding.DaydreamBinding

/**
 * Created by drew on 8/4/17.
 */
class PreviewActivity: LifecycleActivity() {

    private lateinit var mBinding: DaydreamBinding
    private lateinit var mViewModel: DreamViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = DataBindingUtil.setContentView(this, R.layout.daydream)
        mViewModel = fetchViewModel(DreamViewModel::class)

        mViewModel.setSurface(mBinding.videoSurface)

        mViewModel.aspectRatio.observe(this, LiveDataObserver {
            mBinding.surfaceContainer.setAspectRatio(it)
        })
        mViewModel.loading.observe(this, LiveDataObserver {
            mBinding.isLoading = it
        })
        mViewModel.label.observe(this, LiveDataObserver {
            mBinding.labelText = it
        })

    }

    override fun onResume() {
        super.onResume()
        mViewModel.play()
    }

    override fun onStop() {
        super.onStop()
        mViewModel.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.releaseSurface(mBinding.videoSurface)
    }
}