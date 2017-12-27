package org.opensilk.traveltime.data;

/**
 * Response from backend with info needed to create the event watch request.
 *
 * Created by drew on 10/30/17.
 */
public class ChannelResp {
    public String channelId;
    public String address;
    public String token;
    public long expiration;
}
