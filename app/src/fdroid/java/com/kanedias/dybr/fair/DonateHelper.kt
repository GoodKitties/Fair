package com.kanedias.dybr.fair

import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import android.content.Intent
import android.net.Uri
import com.afollestad.materialdialogs.list.listItems


/**
 * Flavor-specific donation helper class. This manages menu option "Donate" in the main activity.
 *
 * @author Kanedias
 *
 * Created on 10.04.18
 */
class DonateHelper(private val activity: AppCompatActivity) {

    fun donate() {
        val options = listOf("Paypal", "Patreon", "Liberapay")
        MaterialDialog(activity)
                .title(R.string.donate)
                .listItems(items = options, selection = {_, _, text -> when (text) {
                    "Paypal" -> openLink("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6CFFYXFT6QT46")
                    "Patreon" -> openLink("https://www.patreon.com/kanedias")
                    "Liberapay" -> openLink("https://liberapay.com/Kanedias")
                }}).show()
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        activity.startActivity(intent)
    }

}