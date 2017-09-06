package org.opensilk.music.ui

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import org.opensilk.common.bindLayout
import org.opensilk.music.R
import org.opensilk.music.databinding.ActivityDrawerBinding
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Created by drew on 5/22/17.
 */
abstract class DrawerSlidingActivity : BaseSlidingActivity(),
        NavigationView.OnNavigationItemSelectedListener {

    protected abstract val mSelfNavActionId: Int
    protected lateinit var mBinding: ActivityDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = bindLayout(R.layout.activity_drawer)

        mBinding.navView.setNavigationItemSelectedListener(this)
        if (mSelfNavActionId != 0) {
            mBinding.navView.setCheckedItem(mSelfNavActionId)
        }

    }

    private var mToolbar: Toolbar? = null
    private var mToolbarToogle: ActionBarDrawerToggle? = null

    fun setToolbar(toolbar: Toolbar) {
        val oldToolbar = mToolbar
        if (oldToolbar != null && oldToolbar != toolbar) {
            clearToolbar(oldToolbar)
        }
        val toggle = ActionBarDrawerToggle(this,
                mBinding.drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        mBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        mToolbarToogle = toggle
    }

    fun clearToolbar(toolbar: Toolbar) {
        if (toolbar != mToolbar) {
            return
        }
        val toggle = mToolbarToogle
        toggle?.let {
            mBinding.drawerLayout.removeDrawerListener(it)
        }
        mToolbar = null
        mToolbarToogle = null
    }

    override fun onBackPressed() {
        if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == mSelfNavActionId) {
            mBinding.drawerLayout.closeDrawer(GravityCompat.START)
            return true
        }

        when (item.itemId) {
            R.id.nav_folders_root -> {
                mMainWorker.schedule({
                    createBackStack(Intent(this, HomeSlidingActivity::class.java))
                    if (mSelfNavActionId != 0) {
                        //change selected item back to our own
                        mBinding.navView.setCheckedItem(mSelfNavActionId)
                    }
                }, NAVDRAWER_LAUNCH_DELAY, TimeUnit.MILLISECONDS)
                // change the active item on the list so the user can see the item changed
                mBinding.navView.setCheckedItem(item.itemId)
            }
            R.id.nav_add_root -> {
                mMainWorker.schedule({
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_OPEN_FOLDER)
                })
            }
        }

        mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult: requestCode %d resultCode %d data %s", requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_FOLDER -> {
                when (resultCode) {
                    RESULT_OK -> {
                        data?.data?.let {
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            contentResolver.takePersistableUriPermission(it, flags)
                        } ?: Timber.e("The returned data was null")
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    protected fun createBackStack(intent: Intent) {
        val bob = TaskStackBuilder.create(this)
        bob.addNextIntentWithParentStack(intent)
        bob.startActivities()
    }
}