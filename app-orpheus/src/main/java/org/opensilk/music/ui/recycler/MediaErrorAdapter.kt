package org.opensilk.music.ui.recycler

import android.databinding.DataBindingUtil
import android.support.design.widget.Snackbar
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import org.opensilk.music.R
import org.opensilk.music.databinding.RecyclerMediaErrorBinding

/**
 * Created by drew on 7/31/16.
 */
class MediaErrorAdapter(
        private val mErrorMsg: String
): RecyclerView.Adapter<MediaErrorVH>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): MediaErrorVH {
        val inflator = LayoutInflater.from(parent!!.context)
        val binding: RecyclerMediaErrorBinding = DataBindingUtil.inflate(inflator,
                R.layout.recycler_media_error, parent, false)
        return MediaErrorVH(binding)
    }

    override fun onBindViewHolder(holder: MediaErrorVH?, position: Int) {
        holder!! //null check
        holder.binding.errorMessage.text = mErrorMsg
        holder.binding.retryBtn.setOnClickListener {
            Snackbar.make(it, "TODO", Snackbar.LENGTH_LONG)
        }
    }

    override fun getItemCount(): Int {
        return 1
    }
}