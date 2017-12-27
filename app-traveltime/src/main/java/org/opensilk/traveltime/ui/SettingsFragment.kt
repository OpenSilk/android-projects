package org.opensilk.traveltime.ui

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.preference.*
import android.text.TextUtils
import android.view.MenuItem
import org.opensilk.traveltime.R

/**
 * Created by drew on 11/25/17.
 */
open class SettingsFragment: PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            startActivity(Intent(activity, SettingsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

}

/**
 * A preference value change listener that updates the preference's summary
 * to reflect its new value.
 */
private object BindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener {
    override fun onPreferenceChange(preference: Preference, value: Any): Boolean {
        val stringValue = value.toString()
        when (preference) {
            is ListPreference -> {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                val listPreference = preference
                val index = listPreference.findIndexOfValue(stringValue)

                // Set the summary to reflect the new value.
                preference.setSummary(if (index >= 0) listPreference.entries[index] else null)

            }
            is RingtonePreference -> {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent)

                } else {
                    val ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue))

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null)
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        val name = ringtone.getTitle(preference.getContext())
                        preference.setSummary(name)
                    }
                }
            }
            else -> {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.summary = stringValue
            }
        }
        return true
    }
}

/**
 * Binds a preference's summary to its value. More specifically, when the
 * preference's value is changed, its summary (line of text below the
 * preference title) is updated to reflect the value. The summary is also
 * immediately updated upon calling this method. The exact display format is
 * dependent on the type of preference.

 * @see .sBindPreferenceSummaryToValueListener
 */
internal fun bindPreferenceSummaryToValue(preference: Preference) {
    // Set the listener to watch for value changes.
    preference.onPreferenceChangeListener = BindPreferenceSummaryToValueListener

    // Trigger the listener immediately with the preference's
    // current value.
    BindPreferenceSummaryToValueListener.onPreferenceChange(preference,
            PreferenceManager
                    .getDefaultSharedPreferences(preference.context)
                    .getString(preference.key, ""))
}