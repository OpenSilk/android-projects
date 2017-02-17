/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.common.recycler

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import java.util.ArrayList
import java.util.Collections

/**
 * Created by drew on 11/20/14.
 */
abstract class RecyclerListAdapter<T, VH : RecyclerView.ViewHolder>
constructor(
        private val items: MutableList<T> = ArrayList<T>(100)
) : RecyclerView.Adapter<VH>() {

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItems(): List<T> {
        return items
    }

    fun getItem(pos: Int): T {
        return items[pos]
    }

    fun addAll(collection: Collection<T>): Boolean {
        val start = items.size
        if (items.addAll(collection)) {
            if (start > 0) {
                notifyItemRangeInserted(start, collection.size)
            } else {
                notifyDataSetChanged()
            }
            return true
        }
        return false
    }

    fun addAll(pos: Int, collection: Collection<T>): Boolean {
        val size = items.size
        if (items.addAll(pos, collection)) {
            if (size > 0) {
                notifyItemRangeInserted(pos, collection.size)
            } else {
                notifyDataSetChanged()
            }
            return true
        }
        return false
    }

    fun replaceAll(collection: Collection<T>): Boolean {
        items.clear()
        if (items.addAll(collection)) {
            notifyDataSetChanged()
            return true
        }
        return false
    }

    fun addItem(item: T): Boolean {
        if (items.add(item)) {
            //notifyItemInserted(items.indexOf(item));
            // bug in StaggeredGrid tries to arrayCopy items.size() + 1 and barfs
            notifyItemRangeInserted(items.indexOf(item), 0)
            return true
        }
        return false
    }

    fun addItem(pos: Int, item: T) {
        items.add(pos, item)
        notifyItemInserted(pos)
    }

    fun removeItem(item: T): Boolean {
        val pos = items.indexOf(item)
        if (items.remove(item)) {
            notifyItemRemoved(pos)
            return true
        }
        return false
    }

    fun removeItem(pos: Int): T {
        val item = items.removeAt(pos)
        notifyItemRemoved(pos)
        return item
    }

    fun indexOf(item: T): Int {
        return items.indexOf(item)
    }

    fun swap(pos1: Int, pos2: Int): Boolean {
        try {
            Collections.swap(items, pos1, pos2)
            notifyItemMoved(pos1, pos2)
            notifyItemMoved(pos2, pos1)
            return true
        } catch (e: IndexOutOfBoundsException) {
            return false
        }
    }

    fun swap(item1: T, item2: T): Boolean {
        return swap(indexOf(item1), indexOf(item2))
    }

    fun move(from: Int, to: Int): Boolean {
        if (from < 0 || to > items.size) {
            return false
        }
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
        return true
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun isEmpty(): Boolean {
        return items.isEmpty()
    }

    companion object {
        //Convenience function
        protected fun inflate(parent: ViewGroup, @LayoutRes id: Int): View {
            return LayoutInflater.from(parent.context).inflate(id, parent, false)
        }
    }

}
