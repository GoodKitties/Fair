package com.kanedias.dybr.fair.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.view.Gravity
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import com.ftinc.scoop.StyleLevel
import com.kanedias.dybr.fair.MainActivity
import com.kanedias.dybr.fair.UserContentListFragment
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


/**
 * Enables or disables all child views recursively descending from [view]
 * @param view view to disable
 * @param enabled true if all views need to be enabled, false otherwise
 */
fun toggleEnableRecursive(view: View, enabled: Boolean) {
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            toggleEnableRecursive(view.getChildAt(i), enabled)
        }
    }

    // dim image if it's image view
    if (view is ImageView) {
        val res = view.drawable.mutate()
        res.colorFilter = when (enabled) {
            true -> null
            false -> PorterDuffColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
        }
    }

    // disable for good
    view.isEnabled = enabled
}

/**
 * Show toast exactly under specified view
 *
 * @param view view at which toast should be located
 * @param text text of toast
 */
fun showToastAtView(view: View, text: String) {
    val toast = Toast.makeText(view.context, text, Toast.LENGTH_SHORT)

    val location = IntArray(2)
    view.getLocationOnScreen(location)

    toast.setGravity(Gravity.TOP or Gravity.START, location[0] - 25, location[1] - 10)
    toast.show()
}

/**
 * Open the URL using the default browser on this device
 */
fun openUrlExternally(ctx: Context, uri: Uri) {
    val pkgMgr = ctx.packageManager
    val intent = Intent(Intent.ACTION_VIEW, uri)

    // detect default browser
    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
    val defaultBrowser = pkgMgr.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

    // use default browser to open the url
    intent.component = with(defaultBrowser.activityInfo) { ComponentName(applicationInfo.packageName, name) }
    ctx.startActivity(intent)
}