package org.opensilk.music.ui.recycler

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import org.opensilk.music.R
import org.opensilk.music.databinding.RecyclerMediaListArtworkBinding
import org.opensilk.music.databinding.RecyclerMediaListArtworkOnelineBinding
import org.opensilk.music.databinding.RecyclerMediaListArtworkOnelineInfoBinding
import org.opensilk.music.ui.ListArtworkOnelineInfoVH
import org.opensilk.music.ui.ListArtworkOnelineVH
import org.opensilk.music.ui.ListArtworkVH
import org.opensilk.music.ui.MediaItemVH
import javax.inject.Inject
import kotlin.properties.Delegates

class MediaItemAdapter
@Inject
constructor() : RecyclerView.Adapter<MediaItemVH<*>>() {

    var items: List<MediaTile> by Delegates.observable(emptyList()) { prop, old, new ->
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: MediaItemVH<*>?, position: Int) {
        holder!!
        holder.onBind(items[position])
    }

    override fun onViewRecycled(holder: MediaItemVH<*>?) {
        super.onViewRecycled(holder)
        holder!!
        holder.onUnbind()
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MediaItemVH<*> {
        val inflator = LayoutInflater.from(parent!!.context)
        val binding: ViewDataBinding = DataBindingUtil.inflate(inflator, viewType, parent, false)
        return when (viewType) {
            R.layout.recycler_media_list_artwork -> {
                ListArtworkVH(binding as RecyclerMediaListArtworkBinding)
            }
            R.layout.recycler_media_list_artwork_oneline -> {
                ListArtworkOnelineVH(binding as RecyclerMediaListArtworkOnelineBinding)
            }
            R.layout.recycler_media_list_artwork_oneline_info -> {
                ListArtworkOnelineInfoVH(binding as RecyclerMediaListArtworkOnelineInfoBinding)
            }
            else -> throw IllegalArgumentException("Unknown viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return items[position].tileLayout
    }
}