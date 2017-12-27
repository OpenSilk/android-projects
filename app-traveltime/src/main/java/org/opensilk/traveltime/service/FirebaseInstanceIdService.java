package org.opensilk.traveltime.service;

import com.google.firebase.iid.FirebaseInstanceId;

/**
 * Called by GMS when the instance id changes
 *
 * Created by drew on 10/29/17.
 */
public class FirebaseInstanceIdService extends com.google.firebase.iid.FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        //TODO tell backend the id has changed. XXX We wont worry about this yet.
    }
}
