package com.kanedias.dybr.fair.misc

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.kanedias.dybr.fair.R

fun FragmentActivity.showFullscreenFragment(frag: Fragment) {
    supportFragmentManager.beginTransaction()
            .addToBackStack("Showing fragment: $frag")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.main_drawer_layout, frag)
            .commit()
}