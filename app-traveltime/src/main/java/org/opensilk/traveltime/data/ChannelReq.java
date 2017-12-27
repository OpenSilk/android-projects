package org.opensilk.traveltime.data;

/**
 * Request sent to the backend when we wish to create a new event watch request
 *
 * Created by drew on 10/30/17.
 */
public class ChannelReq {
    /**
     * Firebase instance token
     */
    public String firebaseToken;
    /**
     * Google account id
     */
    public String googleId;
    /**
     * Calendar id
     */
    public String resourceId;
}
