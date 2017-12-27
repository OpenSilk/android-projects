package org.opensilk.traveltime.service;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import org.opensilk.traveltime.data.ChannelService;

/**
 * Initializes the push notifications for the backend
 *
 * Created by drew on 10/31/17.
 */
public class ChannelInitService extends IntentService {

    @Inject ChannelService service;

    public ChannelInitService() {
        super(ChannelInitService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        service.subscribeEventsChannel();
    }
}
