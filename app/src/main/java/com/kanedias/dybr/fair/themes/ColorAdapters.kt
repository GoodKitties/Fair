package com.kanedias.dybr.fair.themes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.design.widget.TabLayout
import android.support.design.widget.FloatingActionButton
import android.support.annotation.ColorInt
import android.view.View


/**
 * An adapter that dictates how a color property or change is applied
 * to a given view
 */
interface ColorAdapter<T : View> {

    /**
     * Apply the color to the given view
     *
     * @param view      the view to apply the color to
     * @param color     the color to apply
     */
    fun applyColor(view: T, @ColorInt color: Int)

    /**
     * Get the current color for the element
     *
     * @param view      the view to get the color from
     * @return          the current color
     */
    @ColorInt
    fun getColor(view: T): Int

}

/**
 * @author Kanedias
 *
 * Created on 29.07.18
 */
class TabUnderlineAdapter: ColorAdapter<TabLayout> {

    @ColorInt
    var color = Color.TRANSPARENT

    override fun getColor(view: TabLayout): Int {
        return color
    }

    override fun applyColor(view: TabLayout, @ColorInt color: Int) {
        this.color = color
        view.setSelectedTabIndicatorColor(color)
    }
}

class FABColorAdapter : ColorAdapter<FloatingActionButton> {

    override fun applyColor(view: FloatingActionButton, @ColorInt color: Int) {
        val colorStateList = colorToStateList(color)
        view.backgroundTintList = colorStateList
    }

    override fun getColor(view: FloatingActionButton): Int {
        return view.backgroundTintList!!.defaultColor
    }
}

/**
 * The default color adapter that just applies the color to the View background
 *
 */
class DefaultColorAdapter : ColorAdapter<View> {

    override fun applyColor(view: View, @ColorInt color: Int) {
        view.setBackgroundColor(color)
    }

    override fun getColor(view: View): Int {
        val bg = view.background
        return (bg as? ColorDrawable)?.color ?: 0
    }
}
