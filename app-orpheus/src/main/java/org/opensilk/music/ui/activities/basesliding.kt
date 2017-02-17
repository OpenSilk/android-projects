package org.opensilk.music.ui.activities

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.widget.Toast
import dagger.Module
import mortar.ViewPresenter
import org.opensilk.common.bindLayout
import org.opensilk.common.dagger2.ActivityScope
import org.opensilk.common.support.app.ScopedAppCompatActivity
import org.opensilk.music.R
import org.opensilk.music.data.MusicAuthorityModule
import org.opensilk.music.databinding.ActivityDrawerBinding
import org.opensilk.music.ui.presenters.BindingPresenter
import org.opensilk.music.ui.presenters.createBinding
import org.opensilk.music.ui.recycler.MediaErrorAdapter
import rx.subscriptions.CompositeSubscription
import javax.inject.Inject

interface BaseComponent {
}

@dagger.Module
class BaseModule

open class BasePresenter: BindingPresenter() {

    open fun onResume() {

    }

    open fun onPause() {

    }

    //takeView must have been called, null check omitted for convenience
    val binding: ActivityDrawerBinding
        get() = view!! as ActivityDrawerBinding

    open fun startLoadingItems() {
        setRecyclerLoading()
    }

    open fun setRecyclerLoading() {
//        binding.recycler.layoutManager = LinearLayoutManager(context)
//        binding.recycler.setAdapter(MediaLoadingAdapter())
    }

    open fun setRecyclerError(msg: String) {
        binding.recycler.layoutManager = LinearLayoutManager(context)
        binding.recycler.adapter = MediaErrorAdapter(msg)
    }

}

/**
 * Created by drew on 6/28/16.
 */
abstract class BaseSlidingActivity(): ScopedAppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    companion object {
        // delay to launch nav drawer item, to allow close animation to play
        const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L
    }

    protected abstract val selfNavActionId: Int
    protected abstract val mPresenter: BasePresenter
    protected abstract fun injectSelf()

    protected val mMainHandler = Handler()

    protected val mBinding: ActivityDrawerBinding by createBinding(R.layout.activity_drawer)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectSelf()

        setSupportActionBar(mBinding.toolbar)

        val toggle = ActionBarDrawerToggle(this,
                mBinding.drawerLayout, mBinding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        mBinding.navView.setNavigationItemSelectedListener(this)
        if (selfNavActionId != 0) {
            mBinding.navView.setCheckedItem(selfNavActionId)
        }

        mBinding.recycler.setHasFixedSize(true)

        mPresenter.takeView(mBinding)
    }

    override fun onDestroy() {
        super.onDestroy()
        mPresenter.dropView(mBinding)
    }

    override fun onResume() {
        super.onResume()
        mPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        mPresenter.onPause()
    }

    override fun onBackPressed() {
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == selfNavActionId) {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        when (item.itemId) {
            R.id.nav_folders_root -> {
                mMainHandler.postDelayed({
                    when (item.itemId) {
                        R.id.nav_folders_root -> {
                            createBackStack(Intent(this, HomeSlidingActivity::class.java))
                        }
                    }
                }, NAVDRAWER_LAUNCH_DELAY)
                // change the active item on the list so the user can see the item changed
                mBinding.navView.setCheckedItem(item.itemId)
            }
        }

        mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    protected fun createBackStack(intent: Intent) {
        val bob = TaskStackBuilder.create(this)
        bob.addNextIntentWithParentStack(intent)
        bob.startActivities()
    }

    protected fun showErrorToast() {
        Toast.makeText(this, "AN UNHANDLED ERROR OCCURRED", Toast.LENGTH_LONG).show()
    }

}