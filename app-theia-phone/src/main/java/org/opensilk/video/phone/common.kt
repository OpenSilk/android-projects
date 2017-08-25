package org.opensilk.video.phone

import android.app.TaskStackBuilder
import android.arch.lifecycle.*
import android.content.Intent
import android.databinding.DataBindingUtil
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.MenuItem
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import org.opensilk.media.*
import org.opensilk.media.database.MediaDAO
import org.opensilk.media.database.VideoDocumentChange
import org.opensilk.media.loader.doc.DocumentLoader
import org.opensilk.video.*
import org.opensilk.video.phone.databinding.ActivityDrawerBinding
import org.opensilk.video.phone.databinding.RecyclerHeaderItemBinding
import org.opensilk.video.phone.databinding.RecyclerListItemBinding
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

abstract class DrawerActivity: BaseVideoActivity(), NavigationView.OnNavigationItemSelectedListener {

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

        mBinding.recycler.setHasFixedSize(true)

        mDrawerViewModel = fetchViewModel(DrawerActivityViewModel::class)

        mDrawerViewModel.newDocumentId.observe(this, LiveDataObserver { docId ->
            if (docId.isFromTree) {
                startActivity(Intent(this, FolderActivity::class.java)
                        .putExtra(EXTRA_MEDIAID, docId.json))
            } else {
                startActivity(Intent(this, DetailActivity::class.java)
                        .putExtra(EXTRA_MEDIAID, docId.json))
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
            R.id.nav_pick_dir -> {
                mMainHandler.post {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
                            REQUEST_OPEN_FOLDER)
                }
            }
            R.id.nav_pick_file -> {
                mMainHandler.post {
                    startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT)
                            .setType("video/*"), REQUEST_OPEN_FILE)
                }
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
                handleDocumentResult(data, DocumentsContract.Document.MIME_TYPE_DIR)
            } REQUEST_OPEN_FILE -> {
                handleDocumentResult(data, "video/*")
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    protected fun handleDocumentResult(data: Intent?, mime: String) {
        val uri = data?.data
        if (uri == null) {
            Snackbar.make(mBinding.coordinator, "Error. Null uri", Snackbar.LENGTH_INDEFINITE).show()
            return
        }
        contentResolver.takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        mDrawerViewModel.addDocument(DocumentId(treeUri = uri, mimeType = mime))
    }

    protected fun createBackStack(intent: Intent) {
        val bob = TaskStackBuilder.create(this)
        bob.addNextIntentWithParentStack(intent)
        bob.startActivities()
    }

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

    val newDocumentId = MutableLiveData<DocumentId>()

    fun addDocument(docId: DocumentId) {
        mDocumentLoader.document(docId)
                .doOnSuccess {
                    when (it) {
                        is DocDirectoryRef -> {
                            mDatabaseClient.addDirectoryDocument(it)
                            //mDatabaseClient.postChange()
                        }
                        is DocVideoRef -> {
                            mDatabaseClient.addVideoDocument(it)
                            mDatabaseClient.postChange(VideoDocumentChange(it.id))
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

data class HeaderItem(val title: String, val icon: Int)

abstract class BoundViewHolder(root: View): RecyclerView.ViewHolder(root){
    abstract fun unbind()
}

class HeaderViewHolder(val binding: RecyclerHeaderItemBinding) : BoundViewHolder(binding.root) {

    fun bind(headerItem: HeaderItem) {
        binding.titleString = headerItem.title
    }

    override fun unbind() {
    }

}

class ListItemViewHolder(val binding: RecyclerListItemBinding): BoundViewHolder(binding.root),
        View.OnClickListener, View.OnLongClickListener {

    private lateinit var mediaRef: MediaRef

    fun bind(mediaRef: MediaRef) {
        this.mediaRef = mediaRef
        binding.frame.setOnClickListener(this)
        //binding.frame.setOnLongClickListener(this)
        when (mediaRef) {
            is VideoRef -> {
                binding.titleString = mediaRef.meta.title
                binding.subTitleString = mediaRef.meta.subtitle
                if (mediaRef.meta.artworkUri.isEmpty()) {
                    binding.artworkThumb.setImageResource(R.drawable.ic_movie_48dp)
                } else {
                    loadArtwork(mediaRef.meta.artworkUri)
                }
            }
            is FolderRef -> {
                binding.titleString = mediaRef.meta.title
                binding.subTitleString = ""
                binding.artworkThumb.setImageResource(R.drawable.ic_folder_48dp)
            }
            is UpnpDeviceRef -> {
                binding.titleString = mediaRef.meta.title
                binding.subTitleString = mediaRef.meta.subtitle
                if (mediaRef.meta.artworkUri.isEmpty()) {
                    binding.artworkThumb.setImageResource(R.drawable.ic_lan_48dp)
                } else {
                    loadArtwork(mediaRef.meta.artworkUri)
                }
            }
            else -> TODO("Unhandled mediaRef")
        }
    }

    override fun unbind() {
        Glide.with(binding.root.context).clear(binding.artworkThumb)
    }

    private fun loadArtwork(uri: Uri) {
        Glide.with(binding.root.context)
                .asDrawable()
                .apply(RequestOptions().centerCrop())
                .load(uri)
                .into(binding.artworkThumb)
    }

    override fun onClick(v: android.view.View) {
        val ref = mediaRef
        Timber.d("onClick($ref)")
        val activity = v.context.findActivity()
        if (activity is MediaItemClickListener && activity.onClick(ref)) {
            return
        }
        when (ref) {
            is UpnpDeviceRef, is UpnpFolderRef, is DocDirectoryRef -> {
                val intent = Intent(activity, FolderActivity::class.java)
                        .putExtra(EXTRA_MEDIAID, ref.id.json)
                activity.startActivity(intent)
            }
            is UpnpVideoRef, is DocVideoRef -> {
                val intent = Intent(activity, DetailActivity::class.java)
                        .putExtra(EXTRA_MEDIAID, ref.id.json)
                activity.startActivity(intent)
            }
            else -> TODO()
        }
    }

    override fun onLongClick(v: android.view.View?): kotlin.Boolean {
        TODO("not implemented")
    }
}

interface MediaItemClickListener {
    fun onClick(mediaRef: MediaRef): Boolean
    fun onLongClick(mediaRef: MediaRef): Boolean
}