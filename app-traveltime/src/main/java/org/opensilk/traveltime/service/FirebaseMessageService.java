package org.opensilk.traveltime.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import org.opensilk.traveltime.data.Settings;

/**
 * Called by GMS in response to a message from the backend service
 *
 * Created by drew on 10/29/17.
 */
public class FirebaseMessageService extends FirebaseMessagingService {

    @Inject Settings settings;

    @Override
    public void onCreate() {
        AndroidInjection.inject(this);
        super.onCreate();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.i("MessageService", "Received new push message");
        Map<String, String> data = remoteMessage.getData();
        if (data == null) {
            data = Collections.emptyMap();
        }
        Log.i("MessageService", "data="+data);
        CalendarSyncJobService.scheduleSelf(this);
    }
}
