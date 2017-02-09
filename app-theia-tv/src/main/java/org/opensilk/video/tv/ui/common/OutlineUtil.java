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

package org.opensilk.video.tv.ui.common;

import android.view.View;

/**
 * Created by drew on 4/10/16.
 */
public class OutlineUtil {

    private static int sClipRadius;
    private static int sShadowZ;
    private static RoundedRectOutlineProvider sOutlineProvider;

    public static void setOutline(View view) {
        if (sClipRadius == 0) {
            sClipRadius = view.getResources().getDimensionPixelSize(
                    android.support.v17.leanback.R.dimen.lb_rounded_rect_corner_radius);
        }
        if (sOutlineProvider == null) {
            sOutlineProvider = new RoundedRectOutlineProvider(sClipRadius);
        }
        view.setOutlineProvider(sOutlineProvider);
        view.setClipToOutline(true);
        if (sShadowZ == 0) {
            sShadowZ = view.getResources().getDimensionPixelSize(
                    android.support.v17.leanback.R.dimen.lb_playback_controls_z);
        }
        view.setZ(sShadowZ);
    }

}
