package org.opensilk.music.ui.activities

import android.app.TaskStackBuilder
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.view.MenuItem
import org.opensilk.common.lazyBindLayout
import org.opensilk.common.recycler.ItemClickSupport
import org.opensilk.music.R
import org.opensilk.music.databinding.ActivityDrawerBinding
import org.opensilk.music.databinding.SheetPlayingBinding
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * delay to launch nav drawer item, to allow close animation to play
 */
const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L
/**
 * Request code to launch the SAF activity
 */
const private val REQUEST_OPEN_FOLDER = 1001

/**
 * Created by drew on 5/22/17.
 */
abstract class DrawerSlidingActivity : BaseSlidingActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        ItemClickSupport.OnItemClickListener {

    protected abstract val selfNavActionId: Int
    protected val mBinding: ActivityDrawerBinding by lazyBindLayout(R.layout.activity_drawer)
    override val mSheetBinding: SheetPlayingBinding
        get() = mBinding.playingSheet //fetch each time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        mBinding.playingSheet.peekPlaypause.setOnClickListener(this)

        ItemClickSupport.addTo(mBinding.recycler).setOnItemClickListener(this)
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
                    createBackStack(Intent(this, HomeSlidingActivity::class.java))
                    if (selfNavActionId != 0) {
                        //change selected item back to our own
                        mBinding.navView.setCheckedItem(selfNavActionId)
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
                            mDataService.insertRoot(it).subscribe { added ->
                                if (added) {
                                    Snackbar.make(mBinding.coordinator, "New folder added", Snackbar.LENGTH_SHORT)
                                } else {
                                    Snackbar.make(mBinding.coordinator, "Failed to add root", Snackbar.LENGTH_LONG)
                                }
                            }
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