/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * No license
 */
package org.opensilk.video.phone.widget

import android.support.v7.widget.RecyclerView
import android.view.View

import org.opensilk.video.phone.R

//http://www.littlerobots.nl/blog/Handle-Android-RecyclerView-Clicks/
class ItemClickSupport internal constructor(
        private val mRecyclerView: RecyclerView,
        private val mOnItemClickListener: OnItemClickListener? = null,
        private val mOnItemLongClickListener: OnItemLongClickListener? = null
) {
    private val mOnClickListener = View.OnClickListener { v ->
        mOnItemClickListener?.onItemClicked(mRecyclerView,
                mRecyclerView.getChildViewHolder(v).adapterPosition, v)
    }
    private val mOnLongClickListener = View.OnLongClickListener { v ->
        mOnItemLongClickListener?.onItemLongClicked(mRecyclerView,
                mRecyclerView.getChildViewHolder(v).adapterPosition, v) ?: false
    }
    private val mAttachListener = object : RecyclerView.OnChildAttachStateChangeListener {
        override fun onChildViewAttachedToWindow(view: View) {
            view.setOnClickListener(mOnClickListener)
            view.setOnLongClickListener(mOnLongClickListener)
        }

        override fun onChildViewDetachedFromWindow(view: View) {
            view.setOnClickListener(null)
            view.setOnLongClickListener(null)
        }
    }

    init {
        mRecyclerView.addOnChildAttachStateChangeListener(mAttachListener)
    }

    internal fun detach() {
        mRecyclerView.removeOnChildAttachStateChangeListener(mAttachListener)
    }

    interface OnItemClickListener {

        fun onItemClicked(recyclerView: RecyclerView, position: Int, v: View)
    }

    interface OnItemLongClickListener {

        fun onItemLongClicked(recyclerView: RecyclerView, position: Int, v: View): Boolean
    }

}

fun RecyclerView.installItemClickSupport(onClick: ItemClickSupport.OnItemClickListener? = null,
                                         onLongClick: ItemClickSupport.OnItemLongClickListener? = null) {
    removeItemClickSupport()
    setTag(R.id.item_click_support, ItemClickSupport(this, onClick, onLongClick))
}

fun RecyclerView.removeItemClickSupport() {
    (getTag(R.id.item_click_support) as? ItemClickSupport)?.detach()
    setTag(R.id.item_click_support, null)
}