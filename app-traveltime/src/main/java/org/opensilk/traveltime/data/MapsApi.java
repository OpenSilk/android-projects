package org.opensilk.traveltime.data;

import android.text.TextUtils;
import android.util.Log;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;

import javax.inject.Inject;

/**
 * Hides the maps api allowing easier mocking for tests
 *
 * Created by Timberlon on 11/12/2017.
 */
public class MapsApi {

    private final GeoApiContext geoApiContext;
    private final Settings settings;

    @Inject
    public MapsApi(
            GeoApiContext geoApiContext,
            Settings settings
    ) {
        this.geoApiContext = geoApiContext;
        this.settings = settings;
    }

    /**
     * TODO how to add time we want to arrive?
     *
     * @param startAddress
     * @param endAddress
     * @return -1 on error else the duration in milli
     */
    public long getTravelTimeMilli(String startAddress, String endAddress) {
        try {
            Log.i("MapsApi", String.format("Requesting travel time from '%s' to '%s'",
                    startAddress, endAddress));
            return DistanceMatrixApi.newRequest(geoApiContext)
                    .origins(startAddress)
                    .destinations(endAddress)
                    .await()
                    .rows[0].elements[0].duration.inSeconds * 1000;
        } catch (Exception e) {
            Log.e("MapsApi", "Unable to fetch travel time", e);
            return -1;
        }
    }

    public long getTravelTimeFromHome(String newAddress) {
        if (TextUtils.isEmpty(settings.getHomeAddress())) {
            return -1;
        }
        return getTravelTimeMilli(settings.getHomeAddress(), newAddress);
    }
}
