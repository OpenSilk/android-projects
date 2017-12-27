package org.opensilk.traveltime.ui

import android.os.Bundle
import org.opensilk.traveltime.R

/**
 * Created by drew on 11/26/17.
 */
class SettingsDataSync: SettingsFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_data_sync)
        bindPreferenceSummaryToValue(findPreference("sync_frequency"))
    }
}