/*
 * Copyright (c) 2016 OpenSilk Productions LLC.
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

package org.opensilk.video.tv.ui.details;

import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.Row;

import org.opensilk.video.data.VideoFileInfo;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by drew on 3/21/16.
 */
public class DetailsFileInfoRow extends Row {

    public interface Listener {
        void onItemChanged(DetailsFileInfoRow fileInfoRow);
    }

    private ArrayList<WeakReference<Listener>> mListeners;
    private VideoFileInfo mItem;

    public DetailsFileInfoRow(long id, HeaderItem headerItem, VideoFileInfo item) {
        super(id, headerItem);
        mItem = item;
    }

    public DetailsFileInfoRow setItem(VideoFileInfo fileInfo) {
        mItem = fileInfo;
        notifyItemChanged();
        return this;
    }

    public VideoFileInfo getItem() {
        return mItem;
    }

    /**
     * Adds listener for the details page.
     */
    final void addListener(Listener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<WeakReference<Listener>>();
        } else {
            for (int i = 0; i < mListeners.size();) {
                Listener l = mListeners.get(i).get();
                if (l == null) {
                    mListeners.remove(i);
                } else {
                    if (l == listener) {
                        return;
                    }
                    i++;
                }
            }
        }
        mListeners.add(new WeakReference<Listener>(listener));
    }

    /**
     * Removes listener of the details page.
     */
    final void removeListener(Listener listener) {
        if (mListeners != null) {
            for (int i = 0; i < mListeners.size();) {
                Listener l = mListeners.get(i).get();
                if (l == null) {
                    mListeners.remove(i);
                } else {
                    if (l == listener) {
                        mListeners.remove(i);
                        return;
                    }
                    i++;
                }
            }
        }
    }

    /**
     * Notifies listeners for main item change on UI thread.
     */
    final void notifyItemChanged() {
        if (mListeners != null) {
            for (int i = 0; i < mListeners.size();) {
                Listener l = mListeners.get(i).get();
                if (l == null) {
                    mListeners.remove(i);
                } else {
                    l.onItemChanged(this);
                    i++;
                }
            }
        }
    }


}
