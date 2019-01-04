package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

/**
 * Fragment for showing and managing global preferences
 *
 * @author Kanedias
 *
 * Created on 26.04.18
 */
class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.global_prefs)
    }
}