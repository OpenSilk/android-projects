package org.opensilk.video.phone

import android.app.TaskStackBuilder
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.Toolbar
import android.view.Gravity
import android.view.MenuItem
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.opensilk.media.*
import org.opensilk.media.database.DocVideoChange
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.video.AppSchedulers
import org.opensilk.video.LiveDataObserver
import org.opensilk.video.ViewModelKey
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import timber.log.Timber
import javax.inject.Inject

/**
 * delay to launch nav drawer item, to allow close animation to play
 */
const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L
/**
 * Request code to launch the SAF activity
 */
const private val REQUEST_OPEN_FOLDER = 1001
const private val REQUEST_OPEN_FILE = 1002

const val DRAWER_FRAGMENT_CONTAINER = R.id.drawer_layout

/**
 * Created by drew on 9/1/17.
 */
abstract class DrawerActivity: BaseVideoActivity(),
        NavigationView.OnNavigationItemSelectedListener,
        MediaRefClickListener {

    protected var mMainHandler = Handler(Looper.getMainLooper())
    protected open var mSelfNavActionId: Int = 0

    protected lateinit var mBinding: ActivityDrawerBinding
    protected lateinit var mDrawerViewModel: DrawerActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_drawer)

        mBinding.navView.setNavigationItemSelectedListener(this)
        if (mSelfNavActionId != 0) {
            mBinding.navView.setCheckedItem(mSelfNavActionId)
        }

        mDrawerViewModel = fetchViewModel(DrawerActivityViewModel::class)

        mDrawerViewModel.newDocumentId.observe(this, LiveDataObserver { docId ->
            if (docId.isFromTree) {
                startActivity(Intent(this, FolderActivity::class.java)
                        .putMediaIdExtra(docId))
            } else {
                startActivity(Intent(this, DetailActivity::class.java)
                        .putMediaIdExtra(docId))
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        mBinding.unbind()
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
                R.string.nav_drawer_open, R.string.nav_drawer_close)
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
            R.id.nav_home -> {
                mMainHandler.postDelayed ({
                    createBackStack(Intent(this, HomeActivity::class.java))
                    if (mSelfNavActionId != 0) {
                        //change selected item back to our own
                        mBinding.navView.setCheckedItem(mSelfNavActionId)
                    }
                }, NAVDRAWER_LAUNCH_DELAY)
                // change the active item on the list so the user can see the item changed
                mBinding.navView.setCheckedItem(item.itemId)
            }
        }

        mBinding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.d("onActivityResult: requestCode %d resultCode %d data %s", requestCode, resultCode, data)
        if (resultCode != RESULT_OK) {
            return
        }
        when (requestCode) {
            REQUEST_OPEN_FOLDER -> {
                takeUri(data)?.let { uri ->
                    mDrawerViewModel.addDocument(DocDirectoryId(treeUri = uri))
                }
            }
            REQUEST_OPEN_FILE -> {
                takeUri(data)?.let { uri ->
                    mDrawerViewModel.addDocument(DocVideoId(treeUri = uri))
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun takeUri(data: Intent?): Uri? {
        val uri = data?.data
        if (uri == null) {
            Snackbar.make(mBinding.root, "Error. Null uri", Snackbar.LENGTH_INDEFINITE).show()
            return null
        }
        contentResolver.takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        return uri
    }

    override fun onClick(mediaRef: MediaRef): Boolean = when (mediaRef) {
        is DocFileDeviceRef -> {
            startActivityForResult(mediaRef.meta.intent, REQUEST_OPEN_FILE)
            true
        }
        is DocTreeDeviceRef -> {
            startActivityForResult(mediaRef.meta.intent, REQUEST_OPEN_FOLDER)
            true
        }
        else -> false
    }

    override fun onLongClick(mediaRef: MediaRef): Boolean = false
}

fun Context.createBackStack(intent: Intent) {
    val bob = TaskStackBuilder.create(this)
    bob.addNextIntentWithParentStack(intent)
    bob.startActivities()
}

@Module
abstract class DrawerActivityViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(DrawerActivityViewModel::class)
    abstract fun viewModel(vm: DrawerActivityViewModel): ViewModel
}

class DrawerActivityViewModel
@Inject constructor(
        private val mDocumentLoader: DocumentLoader,
        private val mDatabaseClient: MediaDAO
): ViewModel() {

    val newDocumentId = MutableLiveData<DocumentId>()

    fun addDocument(docId: DocumentId) {
        mDocumentLoader.document(docId)
                .doOnSuccess {
                    when (it) {
                        is DocDirectoryRef -> {
                            mDatabaseClient.addDocDirectory(it)
                            mDatabaseClient.postChangeFor(it.id)
                        }
                        is DocVideoRef -> {
                            mDatabaseClient.addDocVideo(it)
                            mDatabaseClient.postChangeFor(it.id)
                        }
                        else -> TODO()
                    }
                }
                .subscribeOn(AppSchedulers.diskIo)
                .subscribe({
                    newDocumentId.postValue(it.id)
                }, {
                    TODO("${it.message}")
                })
    }

}