package org.opensilk.music.ui.activities

import android.app.TaskStackBuilder
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.LinearLayoutManager
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import mortar.ViewPresenter
import org.opensilk.common.bindLayout
import org.opensilk.common.dagger.ActivityScope
import org.opensilk.common.glide.PalettizedBitmapDrawable
import org.opensilk.common.support.app.ScopedAppCompatActivity
import org.opensilk.media.MediaBrowserCallback
import org.opensilk.media._icon
import org.opensilk.media._iconUri
import org.opensilk.media._title
import org.opensilk.music.R
import org.opensilk.music.data.MusicAuthorityModule
import org.opensilk.music.databinding.ActivityDrawerBinding
import org.opensilk.music.playback.PlaybackActions
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

    val binding: ActivityDrawerBinding?
        get() = view as? ActivityDrawerBinding

}

/**
 * delay to launch nav drawer item, to allow close animation to play
 */
const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L

/**
 * Created by drew on 6/28/16.
 */
abstract class BaseSlidingActivity: ScopedAppCompatActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        View.OnClickListener, MediaBrowserCallback.Listener {

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
        mBrowser = MediaBrowser(this, ComponentName(this, PlaybackService::class.java),
                MediaBrowserCallback(this), null)
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

        mBinding.playingSheet.peekPlaypause.setOnClickListener(this)

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
        if (mBrowser.isConnected) {
            mediaController.registerCallback(mPlayingSheetCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        if (mBrowser.isConnected) {
            mediaController.unregisterCallback(mPlayingSheetCallback)
        }
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

    override fun onClick(v: View?) {
        if (v == mBinding.playingSheet.peekPlaypause) {
            if (!mBrowser.isConnected) return
            mediaController.transportControls.sendCustomAction(PlaybackActions.TOGGLE, Bundle())
        }
    }

    protected fun createBackStack(intent: Intent) {
        val bob = TaskStackBuilder.create(this)
        bob.addNextIntentWithParentStack(intent)
        bob.startActivities()
    }

    override fun onBrowserConnected() {
        mediaController = MediaController(this, mBrowser.sessionToken)
        //we get this callback after onResume
        mediaController.registerCallback(mPlayingSheetCallback)
    }

    override fun onBrowserDisconnected() {
        mediaController.unregisterCallback(mPlayingSheetCallback)
        mediaController = null
    }

    private val mPlayingSheetCallback = object: MediaController.Callback() {
        override fun onQueueChanged(queue: MutableList<MediaSession.QueueItem>?) {

        }

        override fun onQueueTitleChanged(title: CharSequence?) {
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            if (state == null) return
            when (state.state) {
                PlaybackState.STATE_PLAYING, PlaybackState.STATE_BUFFERING -> {
                    mBinding.playingSheet.peekPlaypause
                            .setImageResource(R.drawable.ic_pause_black_48dp)
                }
                else -> {
                    mBinding.playingSheet.peekPlaypause
                            .setImageResource(R.drawable.ic_play_black_48dp)
                }
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            if (metadata == null) return
            //set title
            mBinding.playingSheet.peekTitle.text = metadata._title()
            // set icon
            val icon = metadata._iconUri()
            val bitmap = metadata._icon()
            val thumbnail = mBinding.playingSheet.peekThumbnail
            if (icon != null) {
                val opts = RequestOptions().centerCrop(this@BaseSlidingActivity)
                Glide.with(this@BaseSlidingActivity)
                        .`as`(PalettizedBitmapDrawable::class.java)
                        .apply(opts)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .load(icon)
                        .into(thumbnail)
            } else if (bitmap != null) {
                thumbnail.setImageBitmap(bitmap)
            } else {
                thumbnail.setImageResource(R.drawable.ic_music_note_circle_black_48dp)
            }
        }
    }

}