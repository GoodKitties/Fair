package com.kanedias.dybr.fair.ui

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
 * Note: Doesn't work if view hierarchy is just being recreated, like when you launch application again
 * after its activities and fragments were already recycled.
 */
@Suppress("UNCHECKED_CAST")
fun <T: Fragment> FragmentActivity.getTopFragment(type: KClass<T>): T? {
    val clPredicate = { it: Fragment -> it::class.isSubclassOf(type) }
    return supportFragmentManager.fragments.reversed().find(clPredicate) as T?
}

val View.styleLevel : StyleLevel?
    get() {
        val activity = this.context as? MainActivity ?: return null
        val fm = activity.supportFragmentManager

        //first, try to find fragment with style on top of backstack
        for (idx in fm.backStackEntryCount - 1 downTo 0) {
            val entry = fm.getBackStackEntryAt(idx)
            val opsField = entry::class.java.getDeclaredField("mOps").apply { isAccessible = true }
            val ops = opsField.get(entry) as? List<*>

            val lastOp = ops?.lastOrNull() ?: continue
            val fragField = lastOp::class.java.getDeclaredField("fragment").apply { isAccessible = true }
            val fragment = fragField.get(lastOp) as? UserContentListFragment ?: continue

            return fragment.styleLevel
        }

        return activity.styleLevel
    }