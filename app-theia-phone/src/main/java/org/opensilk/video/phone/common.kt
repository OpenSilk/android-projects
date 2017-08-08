package org.opensilk.video.phone

import android.arch.lifecycle.*
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import org.opensilk.media.*
import org.opensilk.video.DatabaseClient
import org.opensilk.video.EXTRA_MEDIAID
import org.opensilk.video.phone.databinding.RecyclerHeaderItemBinding
import org.opensilk.video.phone.databinding.RecyclerListItemBinding
import kotlin.reflect.KClass

fun <T: ViewModel> Fragment.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (activity.application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
        this.lifecycle.addObserver(vm)
    }
    return vm
}

fun <T: ViewModel> BaseVideoActivity.fetchViewModel(clazz: KClass<T>): T {
    val vm = ViewModelProviders.of(this, (application as ViewModelProvider.Factory)).get(clazz.java)
    if (this is LifecycleRegistryOwner && vm is LifecycleObserver) {
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

    override fun getLifecycle(): LifecycleRegistry {
        return lifecycleRegistry
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
            is UpnpDeviceRef -> {
                binding.titleString = mediaRef.meta.title
                binding.subTitleString = mediaRef.meta.subtitle
                if (mediaRef.meta.artworkUri.isEmpty()) {
                    binding.artworkThumb.setImageResource(R.drawable.ic_lan_48dp)
                } else {
                    loadArtwork(mediaRef.meta.artworkUri)
                }
            }
            is UpnpFolderRef -> {
                binding.titleString = mediaRef.meta.title
                binding.subTitleString = ""
                binding.artworkThumb.setImageResource(R.drawable.ic_folder_48dp)

            }
            is UpnpVideoRef -> {
                binding.titleString = mediaRef.meta.title.elseIfBlank(mediaRef.meta.mediaTitle)
                binding.subTitleString = mediaRef.meta.subtitle
                if (mediaRef.meta.artworkUri.isEmpty()) {
                    binding.artworkThumb.setImageResource(R.drawable.ic_movie_48dp)
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
        when (mediaRef) {
            is UpnpDeviceRef, is UpnpFolderRef -> {
                val intent = Intent(v.context, FolderActivity::class.java)
                        .putExtra(EXTRA_MEDIAID, mediaRef.id.json)
                v.context.startActivity(intent)
            }
            is UpnpVideoRef -> {

            }
            else -> TODO()
        }
    }

    override fun onLongClick(v: android.view.View?): kotlin.Boolean {
        TODO("not implemented")
    }
}