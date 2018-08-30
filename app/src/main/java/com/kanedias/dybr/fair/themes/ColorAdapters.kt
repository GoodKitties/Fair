package com.kanedias.dybr.fair.themes

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.design.widget.TabLayout
import android.support.design.widget.FloatingActionButton
import android.support.annotation.ColorInt
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.TextView
import com.ftinc.scoop.adapters.ColorAdapter
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

class CardViewColorAdapter : ColorAdapter<CardView> {

    override fun applyColor(view: CardView, @ColorInt color: Int) {
        view.setCardBackgroundColor(color)
    }

    override fun getColor(view: CardView): Int {
        return view.cardBackgroundColor.defaultColor
    }
}

class ToolbarTextAdapter : ColorAdapter<Toolbar> {

    override fun applyColor(view: Toolbar, @ColorInt color: Int) {
        view.setTitleTextColor(color)
    }

    override fun getColor(view: Toolbar): Int {
        val field = view::class.java.getDeclaredField("mTitleTextView")
        field.isAccessible = true

        val title = field.get(view) as TextView
        return title.textColors.defaultColor
    }
}