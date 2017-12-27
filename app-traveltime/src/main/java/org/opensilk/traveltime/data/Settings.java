package org.opensilk.traveltime.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.opensilk.dagger2.ForApp;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 11/26/17.
 */
@Singleton
public class Settings {

    private final SharedPreferences preferences;

    @Inject
    public Settings(@ForApp Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getSelectedAccount() {
        return preferences.getString("selected_account", "");
    }

    public void setSelectedAccount(String account) {
        preferences.edit().putString("selected_account", account).apply();
    }

    public boolean isChannelInitialized() {
        return preferences.getBoolean("channel_inited", false);
    }

    public void setChannelInitialized(boolean inited) {
        preferences.edit().putBoolean("channel_inited", inited).apply();
    }

    public String getChannelResourceId() {
        return preferences.getString("channel_res_id", "");
    }

    public void setChannelResourceId(String id) {
        preferences.edit().putString("channel_res_id", id).apply();
    }

    public String getHomeAddress() {
        return preferences.getString("home_place", "");
    }

    public void setNumberNormalEvents(int num) {
        preferences.edit().putInt("cal_num_events", num).apply();
    }

    public int getNumberNormalEvents() {
        return preferences.getInt("cal_num_events", 0);
    }
}
