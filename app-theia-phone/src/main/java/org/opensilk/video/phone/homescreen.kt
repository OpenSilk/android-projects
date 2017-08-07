package org.opensilk.video.phone

import android.databinding.DataBindingUtil
import android.os.Bundle
import org.opensilk.video.HomeViewModel
import org.opensilk.video.phone.databinding.ActivityDrawerBinding

class HomeActivity : BaseVideoActivity() {

    private lateinit var mBinding: ActivityDrawerBinding
    private lateinit var mViewModel: HomeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_drawer)
        mViewModel = fetchViewModel(HomeViewModel::class)
        
    }

}
