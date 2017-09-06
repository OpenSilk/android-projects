package org.opensilk.music.ui

import android.os.Bundle
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.opensilk.common.bindLayout
import org.opensilk.music.R
import org.opensilk.music.databinding.ActivityDetailsBinding

/**
 *
 */
@Module
abstract class DetailModule {
    @ContributesAndroidInjector
    abstract fun activity(): DetailSlidingActivity
}

/**
 *
 */
class DetailSlidingActivity : BaseSlidingActivity() {

    private lateinit var mBinding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = bindLayout(R.layout.activity_details)
    }
}