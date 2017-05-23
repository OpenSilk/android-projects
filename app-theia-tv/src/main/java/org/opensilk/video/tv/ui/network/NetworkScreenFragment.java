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

package org.opensilk.video.tv.ui.network;

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v17.leanback.app.BrowseFragment;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;

import org.opensilk.common.dagger.FuncsKt;
import org.opensilk.common.dagger2.DaggerFuncsKt;
import org.opensilk.video.data.IndexedFoldersLoader;
import org.opensilk.video.data.UpnpDevicesLoader;
import org.opensilk.video.tv.ui.common.MediaItemClickListener;

import java.util.List;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 4/10/16.
 */
public class NetworkScreenFragment extends BrowseFragment {

    NetworkScreenComponent mComponent;
    @Inject IndexedFoldersLoader mFoldersLoader;
    @Inject UpnpDevicesLoader mUpnpLoader;
    @Inject IndexedFoldersAdapter mFoldersAdapter;
    @Inject UpnpDevicesAdapter mUpnpAdapter;

    CompositeSubscription mSubscriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NetworkActivityComponent activityComponent = FuncsKt.getDaggerComponent(getActivity());
        NetworkScreenModule module = new NetworkScreenModule();
        mComponent = activityComponent.newNetworkScreenComponent(module);
        mSubscriptions = new CompositeSubscription();
        mComponent.inject(this);

        setHeadersState(HEADERS_DISABLED);
        setTitle("Folders");
        loadRows();
        setOnItemViewClickedListener(new MediaItemClickListener());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscriptions.unsubscribe();
        mSubscriptions = null;

        mComponent = null;
    }

    private void loadRows() {

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        HeaderItem networkHeader = new HeaderItem("Indexed Folders");
        rowsAdapter.add(new ListRow(networkHeader, mFoldersAdapter));
        Subscription foldersSubscription = mFoldersLoader.getListObservable()
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> list) {
                        mFoldersAdapter.swap(list);
                    }
                });
        mSubscriptions.add(foldersSubscription);

        HeaderItem favoritesHeader = new HeaderItem("Content Directories");
        rowsAdapter.add(new ListRow(favoritesHeader, mUpnpAdapter));
        Subscription upnpSubscription = mUpnpLoader.getObservable()
                .subscribe(new Action1<MediaBrowser.MediaItem>() {
                    @Override
                    public void call(MediaBrowser.MediaItem item) {
                        mUpnpAdapter.add(item);
                    }
                });
        mSubscriptions.add(upnpSubscription);

        setAdapter(rowsAdapter);

    }
}
