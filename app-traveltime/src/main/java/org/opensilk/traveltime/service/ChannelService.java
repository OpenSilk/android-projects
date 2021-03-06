package org.opensilk.traveltime.service;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.Log;

import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Channel;

import org.opensilk.traveltime.api.AuthHelper;
import org.opensilk.traveltime.api.BackendApi;
import org.opensilk.traveltime.api.ChannelNewResp;
import org.opensilk.traveltime.data.Settings;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Created by drew on 11/26/17.
 */

public class ChannelService {

    private final AuthHelper authHelper;
    private final BackendApi backendApi;
    private final Settings settings;

    @Inject
    public ChannelService(AuthHelper authHelper, BackendApi backendApi, Settings settings) {
        this.authHelper = authHelper;
        this.backendApi = backendApi;
        this.settings = settings;
    }

    public void subscribeEventsChannel() {
        if (!authHelper.isAccountSelected()) {
            return;
        }
        if (TextUtils.isEmpty(authHelper.getFirebaseToken())) {
            return;
        }
        //asks the backend for information to build the watch channel
        //then subscribes to the watch
        //ChannelNewReq req = new ChannelNewReq();
        //req.setFirebaseToken(authHelper.getFirebaseToken());
        //req.setGoogleId(authHelper.getAccountId());
        //req.setResourceId(AuthHelper.CALENDAR_ID);
        /*
        backendApi.prepareChannel(req).subscribe(resp -> {
            Channel channel = performWatch(buildChannel(resp));
            //TODO save resourceId for unsubscribe;
            settings.setChannelInitialized(true);
            settings.setChannelResourceId(channel.getResourceId());
        }, err -> {
            settings.setChannelInitialized(false);
            Log.e("ERROR", "Unable to create channel", err);
            if (!"".equals(settings.getChannelResourceId())) {
                Channel stop = new Channel();
                stop.setId(authHelper.getFirebaseToken());
                stop.setResourceId(settings.getChannelResourceId());
                if (!stopChannel(stop)) {
                    Log.e("ERROR", "Unable to unsubscribe from previous channel");
                }
            }
        });
        */
    }

    @VisibleForTesting
    Channel buildChannel(ChannelNewResp resp) {
        Channel channel = new Channel();
        channel.setId(resp.getChannelId());
        channel.setType("web_hook");
        channel.setAddress(resp.getAddress());
        channel.setToken(resp.getToken());
        channel.setExpiration(System.currentTimeMillis() + 1000000000L);
        return channel;
    }

    @VisibleForTesting
    Channel performWatch(Channel channel) throws IOException {
        Calendar.Events.Watch watch = authHelper.calendarEvents()
                .watch(AuthHelper.CALENDAR_ID, channel);
        return watch.execute();
    }

    boolean stopChannel(Channel channel) {
        try {
            authHelper.calendar().channels().stop(channel).execute();
            return true;
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage(), e);
            return false;
        }
    }

}
