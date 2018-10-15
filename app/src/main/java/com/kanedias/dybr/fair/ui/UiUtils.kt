package com.kanedias.dybr.fair.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable




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