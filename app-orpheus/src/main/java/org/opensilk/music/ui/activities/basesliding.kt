package org.opensilk.music.ui.activities

import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.browse.MediaBrowser
import android.media.session.MediaController
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
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.support.app.ScopedAppCompatActivity
import org.opensilk.music.R
import org.opensilk.music.data.MusicAuthorityModule
import org.opensilk.music.databinding.ActivityDrawerBinding
import org.opensilk.music.playback.PlaybackService
import org.opensilk.music.ui.presenters.BindingPresenter
import org.opensilk.music.ui.presenters.createBinding
import org.opensilk.music.ui.recycler.MediaErrorAdapter
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.subscriptions.CompositeSubscription
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 *
 */
interface BaseComponent

/**
 *
 */
@dagger.Module
class BaseModule

/**
 *
 */
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
 * delay to launch nav drawer item, to allow close animation to play
 */
const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L

/**
 * Created by drew on 6/28/16.
 */
abstract class BaseSlidingActivity: ScopedAppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected abstract val selfNavActionId: Int
    protected abstract val mPresenter: BasePresenter
    protected abstract fun injectSelf()

    protected val mBinding: ActivityDrawerBinding by createBinding(R.layout.activity_drawer)

    protected lateinit var mMainWorker: Scheduler.Worker
    protected lateinit var mBrowser: MediaBrowser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        injectSelf()

        mMainWorker = AndroidSchedulers.mainThread().createWorker()
        mBrowser = MediaBrowser(this, ComponentName(this, PlaybackService::class.java), object : MediaBrowser.ConnectionCallback() {
            override fun onConnected() {
                onBrowserConnected()
            }
        }, null)
        mBrowser.connect()

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
        mMainWorker.unsubscribe()
        mBrowser.disconnect()
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
                mMainWorker.schedule({
                    when (item.itemId) {
                        R.id.nav_folders_root -> {
                            createBackStack(Intent(this, HomeSlidingActivity::class.java))
                        }
                    }
                    if (selfNavActionId != 0) {
                        //change selected item back to our own
                        mBinding.navView.setCheckedItem(selfNavActionId)
                    }
                }, NAVDRAWER_LAUNCH_DELAY, TimeUnit.MILLISECONDS)
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

    fun onBrowserConnected() {
        mediaController = MediaController(this, mBrowser.sessionToken)
    }

}