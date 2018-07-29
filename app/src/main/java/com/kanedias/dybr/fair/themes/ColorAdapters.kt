package com.kanedias.dybr.fair.themes

import android.graphics.Color
import android.support.design.widget.TabLayout
import com.ftinc.scoop.adapters.ColorAdapter
import android.support.v4.view.ViewCompat.getBackgroundTintList
import android.support.design.widget.FloatingActionButton
import android.content.res.ColorStateList
import android.support.annotation.ColorInt
import com.ftinc.scoop.util.Utils


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
        val colorStateList = Utils.colorToStateList(color)
        view.backgroundTintList = colorStateList
    }

    override fun getColor(view: FloatingActionButton): Int {
        return view.backgroundTintList!!.defaultColor
    }
}