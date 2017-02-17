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

package org.opensilk.video.tv.ui.playback;

import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.PixelFormat;
import android.media.browse.MediaBrowser;
import android.media.session.MediaController;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.ViewGroup;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.app.ScopedActivity;
import org.opensilk.common.dagger2.DaggerFuncsKt;
import org.opensilk.video.R;
import org.opensilk.video.VideoAppComponent;
import org.opensilk.video.data.MediaDescriptionUtil;
import org.opensilk.video.databinding.ActivityPlaybackBinding;
import org.opensilk.video.playback.PlaybackService;
import org.opensilk.video.tv.ui.details.DetailsActivity;
import org.videolan.libvlc.IVLCVout;

import java.lang.reflect.Field;

import javax.inject.Inject;

import mortar.MortarScope;
import timber.log.Timber;

/**
 * Created by drew on 3/20/16.
 */
public class PlaybackActivity extends ScopedActivity implements IVLCVout.Callback {

    public static String ACTION_PLAY = "action_play";
    public static String ACTION_RESUME = "action_resume";

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    PlaybackActivityComponent mComponent;
    ActivityPlaybackBinding mBinding;

    @Inject PlaybackService mPlayback;

    boolean mSurfacesAttached;

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;

    @Override
    protected void onCreateScope(MortarScope.Builder builder) {
        VideoAppComponent appComponent = DaggerFuncsKt.getDaggerComponent(getApplicationContext());
        PlaybackActivityModule activityModule = new PlaybackActivityModule(getMediaItem());
        PlaybackActivityComponent activityComponent = PlaybackActivityComponent.FACTORY.call(appComponent, activityModule);
        DaggerFuncsKt.withDaggerComponent(builder, activityComponent);
        mComponent = activityComponent;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Timber.d("onNewIntent(%s)", intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mComponent.inject(this);

        mPlayback.onCreate();
        setMediaController(new MediaController(this, mPlayback.getMediaSession().getSessionToken()));

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_playback);

        mBinding.subtitlesSurface.setZOrderMediaOverlay(true);
        mBinding.subtitlesSurface.getHolder().setFormat(PixelFormat.TRANSLUCENT);

        setupSurfaces();
        loadMediaItem();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayback.onDestroy();
        mBinding = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing() || !requestVisibleBehind(true)) {
            stopPlayback();
        }
    }

    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
        stopPlayback();
//        finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Field[] fields = KeyEvent.class.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().startsWith("KEYCODE")) {
                try {
                    int val = f.getInt(null);
                    if (val == keyCode) {
                        Timber.d("onKeyDown(%s)", f.getName());
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
//        switch (keyCode) {
//            case KeyEvent.KEYCODE_DPAD_RIGHT:
//            case KeyEvent.KEYCODE_DPAD_LEFT: {
//                boolean pos = keyCode == KeyEvent.KEYCODE_DPAD_RIGHT;
//                getMediaController().getTransportControls().sendCustomAction(
//                        PlaybackService.ACTION.SEEK_DELTA,
//                        BundleHelper.b().putInt(pos ? 10000 : -10000).get());
//                return true;
//            }
//        }
//        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
//            getMediaController().getTransportControls().skipToNext();
//            return true;
//        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
//            getMediaController().getTransportControls().skipToPrevious();
//            return true;
//        }
        return super.onKeyDown(keyCode, event);
    }

    void setupSurfaces() {
        final IVLCVout ivlcVout = mPlayback.getVLCVout();
        ivlcVout.setVideoView(mBinding.playerSurface);
        ivlcVout.setSubtitlesView(mBinding.subtitlesSurface);
        ivlcVout.attachViews();
        mSurfacesAttached = true;
        ivlcVout.addCallback(this);
        changeSurfaceLayout();
    }

    void cleanupSurfaces() {
        final IVLCVout ivlcVout = mPlayback.getVLCVout();
        ivlcVout.removeCallback(this);
        ivlcVout.detachViews();
        mSurfacesAttached = false;
    }

    void stopPlayback() {
        getMediaController().getTransportControls().stop();
        cleanupSurfaces();
        //TODO save position
    }

    private void changeSurfaceLayout() {

        // get screen size
        int sw = getWindow().getDecorView().getWidth();
        int sh = getWindow().getDecorView().getHeight();

        mPlayback.getVLCVout().setWindowSize(sw, sh);

        double dw = sw, dh = sh;
        boolean isPortrait;

        // getWindow().getDecorView() doesn't always take orientation into account, we have to correct the values
        isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }

        // sanity check
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            Timber.e("Invalid surface size");
            return;
        }

        // compute the aspect ratio
        double ar, vw;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mSarNum / mSarDen;
            ar = vw / mVideoVisibleHeight;
        }

        // compute the display aspect ratio
        double dar = dw / dh;

        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_HORIZONTAL:
                dh = dw / ar;
                break;
            case SURFACE_FIT_VERTICAL:
                dw = dh * ar;
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }

        // set display size
        ViewGroup.LayoutParams lp = mBinding.playerSurface.getLayoutParams();
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        mBinding.playerSurface.setLayoutParams(lp);
        if (mBinding.subtitlesSurface != null) {
            mBinding.subtitlesSurface.setLayoutParams(lp);
        }

        // set frame size (crop if necessary)
        lp = mBinding.playerSurfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        mBinding.playerSurfaceFrame.setLayoutParams(lp);

        mBinding.playerSurface.invalidate();
        if (mBinding.subtitlesSurface != null) {
            mBinding.subtitlesSurface.invalidate();
        }
    }

    private void loadMediaItem() {
        Bundle b = new Bundle();
        if (StringUtils.equals(ACTION_RESUME, getIntent().getAction())) {
            b.putBoolean("resume", true);
        }
        Uri mediaUri = MediaDescriptionUtil.getMediaUri(getMediaItem().getDescription());
        getMediaController().getTransportControls().playFromUri(mediaUri, b);
    }

    private MediaBrowser.MediaItem getMediaItem() {
        return getIntent().getParcelableExtra(DetailsActivity.MEDIA_ITEM);
    }

    /*
     * start IVLCVout.Callback
     */

    @Override
    public void onNewLayout(IVLCVout ivlcVout, int width, int height, int visibleWidth,
                            int visibleHeight, int sarNum, int sarDen) {
        Timber.d("onNewLayout()");
        if (width * height == 0)
            return;

        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth  = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mSarNum = sarNum;
        mSarDen = sarDen;
        changeSurfaceLayout();
    }

    @Override
    public void onSurfacesCreated(IVLCVout ivlcVout) {
        Timber.d("onSurfacesCreated()");
    }

    @Override
    public void onSurfacesDestroyed(IVLCVout ivlcVout) {
        Timber.d("onSurfacesDestroyed()");
        mSurfacesAttached = false;
    }

    @Override
    public void onHardwareAccelerationError(IVLCVout ivlcVout) {
        Timber.d("onHardwareAccelerationError()");
    }

    /*
     * end IVLCVout.Callback
     */


}
