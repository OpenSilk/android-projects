package org.opensilk.video.phone

import android.app.TaskStackBuilder
import android.arch.lifecycle.*
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.DocVideoChange
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.video.*
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import org.opensilk.video.phone.databinding.RecyclerBinding
import timber.log.Timber
import javax.inject.Inject
import kotlin.reflect.KClass

/**
 * delay to launch nav drawer item, to allow close animation to play
 */
const private val NAVDRAWER_LAUNCH_DELAY: Long = 250L
/**
 * Request code to launch the SAF activity
 */
const private val REQUEST_OPEN_FOLDER = 1001
const private val REQUEST_OPEN_FILE = 1002

fun <T: ViewModel> Fragment.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (activity.application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

fun <T: ViewModel> Fragment.fetchActivityViewModel(clazz: KClass<T>): T =
        ViewModelProviders.of(activity, (activity.application) as ViewModelProvider.Factory).get(clazz.java)

fun <T: ViewModel> BaseVideoActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

/**
 * Created by drew on 6/1/17.
 */
abstract class BaseVideoActivity: AppCompatActivity(), LifecycleRegistryOwner {

    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = lifecycleRegistry

}

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

        setSupportActionBar(mBinding.toolbar)

        val toggle = ActionBarDrawerToggle(this,
                mBinding.drawerLayout, mBinding.toolbar,
                R.string.nav_drawer_open, R.string.nav_drawer_close)
        mBinding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        mBinding.navView.setNavigationItemSelectedListener(this)
        if (mSelfNavActionId != 0) {
            mBinding.navView.setCheckedItem(mSelfNavActionId)
        }

        mDrawerViewModel = fetchViewModel(DrawerActivityViewModel::class)

        mDrawerViewModel.loadError.observe(this, LiveDataObserver {
            Snackbar.make(mBinding.coordinator, it, Snackbar.LENGTH_INDEFINITE).show()
        })
        mDrawerViewModel.mediaTitle.observe(this, LiveDataObserver {
            title = it
        })
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
            Snackbar.make(mBinding.coordinator, "Error. Null uri", Snackbar.LENGTH_INDEFINITE).show()
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
    @Binds @IntoMap @ViewModelKey(DrawerActivityViewModel::class)
    abstract fun viewModel(vm: DrawerActivityViewModel): ViewModel
}

class DrawerActivityViewModel
@Inject constructor(
        private val mDocumentLoader: DocumentLoader,
        private val mDatabaseClient: MediaDAO
): ViewModel() {

    val mediaTitle = MutableLiveData<String>()
    val loadError = MutableLiveData<String>()

    val newDocumentId = MutableLiveData<DocumentId>()

    fun addDocument(docId: DocumentId) {
        mDocumentLoader.document(docId)
                .doOnSuccess {
                    when (it) {
                        is DocDirectoryRef -> {
                            mDatabaseClient.addDocDirectory(it)
                            //mDatabaseClient.postChange()
                        }
                        is DocVideoRef -> {
                            mDatabaseClient.addDocVideo(it)
                            mDatabaseClient.postChange(DocVideoChange(it.id))
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

open class RecyclerFragment: LifecycleFragment() {
    protected lateinit var mBinding: RecyclerBinding

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.recycler, container, false)
        mBinding.recycler.setHasFixedSize(true)
        return mBinding.root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated()")
    }

    override fun onDestroyView() {
        Timber.d("onDestroyView()")
        super.onDestroyView()
        mBinding.swipeRefresh.isRefreshing = false
        //this prevents ghosting when popping the backstack while
        //the loading indicator is showing
        mBinding.swipeRefresh.removeAllViews()
        mBinding.unbind()
    }

}
