package com.kanedias.dybr.fair.misc

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.RecyclerView
import com.ftinc.scoop.StyleLevel
import com.kanedias.dybr.fair.MainActivity
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.UserContentListFragment
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

fun FragmentActivity.showFullscreenFragment(frag: Fragment) {
    supportFragmentManager.beginTransaction()
            .addToBackStack("Showing fragment: $frag")
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .add(R.id.main_layout, frag)
            .commit()
}

fun RecyclerView.setMaxFlingVelocity(velocity: Int) {
    val field = RecyclerView::class.java.getDeclaredField("mMaxFlingVelocity")
    field.isAccessible = true
    field.setInt(this, velocity)
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

        // first, check if this view's parent fragment is itself styled
        for (fragment in fm.fragments) {
            val styled = fragment as? UserContentListFragment ?: continue

            var parent = this
            while (parent.parent != null && parent.parent is View) {
                if (parent.parent === fragment.view)
                    return styled.styleLevel

                parent = parent.parent as View
            }
        }

        // second, try to find fragment with style on top of backstack
        for (idx in fm.backStackEntryCount - 1 downTo 0) {
            val entry = fm.getBackStackEntryAt(idx)
            val opsField = FragmentTransaction::class.java.getDeclaredField("mOps").apply { isAccessible = true }
            val ops = opsField.get(entry) as? List<*>

            val lastOp = ops?.lastOrNull() ?: continue
            val fragField = lastOp::class.java.getDeclaredField("mFragment").apply { isAccessible = true }
            val fragment = fragField.get(lastOp) as? UserContentListFragment ?: continue

            return fragment.styleLevel
        }

        return activity.styleLevel
    }