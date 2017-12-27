package org.opensilk.traveltime.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import org.opensilk.traveltime.R

const val REQUEST_HOME = 10032
const val REQUEST_WORK = 10033

/**
 * Created by drew on 11/25/17.
 */
class SettingsPlaces: SettingsFragment(), Preference.OnPreferenceClickListener {

    lateinit var mHome: Preference
    lateinit var mWork: Preference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_places)
        mHome = findPreference("home_place")
        val homeAddr = preferenceManager.sharedPreferences.getString("home_place", "")
        mHome.summary = homeAddr
        mHome.onPreferenceClickListener = this

        mWork = findPreference("work_place")
        val workAddr = preferenceManager.sharedPreferences.getString("work_place", "")
        mWork.summary = workAddr
        mWork.onPreferenceClickListener = this

    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val intent = PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                .build(activity)
        val code: Int = when {
            preference === mHome -> REQUEST_HOME
            preference === mWork -> REQUEST_WORK
            else -> throw IllegalArgumentException("Unknown preference")
        }
        startActivityForResult(intent, code)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK) {
            return
        }
        val place = PlaceAutocomplete.getPlace(activity, data)
        when (requestCode) {
            REQUEST_HOME -> {
                saveAddr("home_place", place.address.toString())
            }
            REQUEST_WORK -> {
                saveAddr("work_place", place.address.toString())
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun saveAddr(pref: String, addr: String) {
        preferenceManager.sharedPreferences.edit()
                .putString(pref, addr)
                .apply()
        findPreference(pref).summary = addr
    }

}

