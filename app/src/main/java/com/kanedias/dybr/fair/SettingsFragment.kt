package com.kanedias.dybr.fair

import android.os.Bundle
import android.preference.PreferenceFragment

/**
 * Fragment for showing and managing global preferences
 *
 * @author Kanedias
 *
 * Created on 26.04.18
 */
class SettingsFragment : PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.global_prefs)
    }
}