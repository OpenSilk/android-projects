package org.opensilk.traveltime.data;

import io.reactivex.Single;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

/**
 * The Backend API is how we communicate with our backend
 *
 * Created by drew on 10/29/17.
 */
public interface BackendApi {
    /**
     * Backend will generate a channelId and provide the subscription url
     * @param req
     * @return
     */
    @POST("channel/new")
    @Headers("Content-Type: application/json")
    Single<ChannelResp> prepareChannel(@Body ChannelReq req);
}
