package org.opensilk.music.ui

import android.content.Context
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import android.view.View
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import org.opensilk.music.R
import javax.inject.Inject

/**
 * Created by drew on 8/1/16.
 */
@Module
abstract class FolderModule {
    @ContributesAndroidInjector
    abstract fun folderFragment(): FolderSlidingActivity
}

/**
 *
 */
class FolderSlidingActivity: DrawerSlidingActivity() {

    override val mSelfNavActionId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

}

class FolderFragment: RecyclerFragment(), Toolbar.OnMenuItemClickListener {

    @Inject lateinit var mAdapter: SwappingAdapter

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBinding.toolbar.inflateMenu(R.menu.folder_sort_by)
        mBinding.toolbar.inflateMenu(R.menu.view_as)
        mBinding.toolbar.setOnMenuItemClickListener(this)
        mBinding.recycler.adapter = mAdapter
    }

    override fun onMenuItemClick(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_sort_by_az -> {
            true //ret
        }
        R.id.menu_sort_by_za -> {
            true //ret
        }
        R.id.menu_view_as_grid -> {
            true
        }
        R.id.menu_view_as_simple -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
