/*****************************************************************************
 * VLCInstance.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.opensilk.video.playback;

import android.content.Context;
import android.widget.Toast;

import org.opensilk.common.dagger.DaggerService;
import org.opensilk.common.dagger.ForApplication;
import org.opensilk.video.VideoAppComponent;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.VLCUtil;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class VLCInstance {

    private static LibVLC sLibVLC = null;

    private final LibVLC libVLC;

    @Inject
    public VLCInstance(@ForApplication Context context) {
        if (!VLCUtil.hasCompatibleCPU(context)) {
            String err = VLCUtil.getErrorMsg();
            Timber.e(err);
            throw new RuntimeException(err);
        }
        libVLC = new LibVLC(VLCOptions.getLibOptions(context));
        LibVLC.setOnNativeCrashListener(() -> {
            Timber.e("TODO handle native crash");
        });
    }

    public LibVLC get() {
        return libVLC;
    }

    /** A set of utility functions for the VLC application */
    @Deprecated
    public synchronized static LibVLC get(final Context context) throws IllegalStateException {
        VideoAppComponent appComponent = DaggerService.getDaggerComponent(context);
        return appComponent.vlcInstance().get();
    }

    public static synchronized boolean testCompatibleCPU(Context context) {
        if (sLibVLC == null && !VLCUtil.hasCompatibleCPU(context)) {
            Toast.makeText(context, "INCOMPATIBLE CPU DETECTED", Toast.LENGTH_LONG).show();
            Timber.e("INCOMPATIBLE CPU DETECTED");
            return false;
        } else {
            return true;
        }
    }
}
