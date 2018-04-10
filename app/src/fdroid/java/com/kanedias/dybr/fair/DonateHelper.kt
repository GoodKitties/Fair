package com.kanedias.dybr.fair

import android.support.v7.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog

/**
 * Flavor-specific donation helper class. This manages menu option "Donate" in the main activity.
 *
 * @author Kanedias
 *
 * Created on 10.04.18
 */
class DonateHelper(private val activity: AppCompatActivity) {

    fun donate() {
        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.donate)
                .show()
    }

}