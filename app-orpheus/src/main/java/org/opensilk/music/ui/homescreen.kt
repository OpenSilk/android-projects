package org.opensilk.music.ui

import android.os.Bundle
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class HomeModule() {
    @ContributesAndroidInjector
    abstract fun home(): HomeSlidingActivity
}

/**
 *
 */
class HomeSlidingActivity : DrawerSlidingActivity() {

    override val mSelfNavActionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}

class HomeFragment: RecyclerFragment() {

}
