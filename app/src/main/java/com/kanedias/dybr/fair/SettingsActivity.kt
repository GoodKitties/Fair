package com.kanedias.dybr.fair

import android.os.Bundle
import android.preference.PreferenceActivity
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import butterknife.BindView
import butterknife.ButterKnife

/**
 * Activity for holding and showing preference fragments
 *
 * @author Kanedias
 *
 * Created on 26.04.18
 */
class SettingsActivity: AppCompatActivity() {

    @BindView(R.id.pref_toolbar)
    lateinit var prefToolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)
        ButterKnife.bind(this)

        setSupportActionBar(prefToolbar)
        fragmentManager.beginTransaction().replace(R.id.pref_content_frame, SettingsFragment()).commit()
    }

}