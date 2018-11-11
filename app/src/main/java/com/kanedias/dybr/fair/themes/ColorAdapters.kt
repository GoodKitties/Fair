package com.kanedias.dybr.fair.themes

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.design.widget.TabLayout
import android.support.design.widget.FloatingActionButton
import android.support.annotation.ColorInt
import android.support.v4.graphics.ColorUtils
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.view.TintableBackgroundView
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
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

class FabColorAdapter : ColorAdapter<FloatingActionButton> {

    override fun applyColor(view: FloatingActionButton, @ColorInt color: Int) {
        val colorStateList = Utils.colorToStateList(color)
        view.backgroundTintList = colorStateList
    }

    override fun getColor(view: FloatingActionButton): Int {
        return view.backgroundTintList!!.defaultColor
    }
}

class FabIconAdapter : ColorAdapter<FloatingActionButton> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: FloatingActionButton, @ColorInt color: Int) {
        this.color = color

        val colorStateList = Utils.colorToStateList(color)
        DrawableCompat.setTintList(view.drawable, colorStateList)
    }

    override fun getColor(view: FloatingActionButton): Int {
        return color
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

        val title = field.get(view) as TextView?
        return title?.textColors?.defaultColor ?: Color.TRANSPARENT
    }
}

class ToolbarIconAdapter : ColorAdapter<Toolbar> {

    override fun applyColor(view: Toolbar, @ColorInt color: Int) {
        val icon = view.navigationIcon as DrawerArrowDrawable?
        icon?.color = color
    }

    override fun getColor(view: Toolbar): Int {
        val icon = view.navigationIcon as DrawerArrowDrawable?
        return icon?.color ?: Color.TRANSPARENT
    }
}

class TextViewLinksAdapter: ColorAdapter<TextView> {

    override fun applyColor(view: TextView, color: Int) {
        view.setLinkTextColor(color)
    }

    override fun getColor(view: TextView): Int {
        return view.linkTextColors.defaultColor
    }
}

class CheckBoxAdapter: ColorAdapter<CheckBox> {

    override fun applyColor(view: CheckBox, color: Int) {
        CompoundButtonCompat.setButtonTintList(view, Utils.colorToStateList(color))
    }

    override fun getColor(view: CheckBox): Int {
        return CompoundButtonCompat.getButtonTintList(view)?.defaultColor ?: Color.TRANSPARENT
    }

}

class EditTextAdapter: ColorAdapter<EditText> {

    override fun applyColor(view: EditText, color: Int) {
        view.setTextColor(color)
    }

    override fun getColor(view: EditText): Int {
        return view.textColors.defaultColor
    }
}

class EditTextLineAdapter: ColorAdapter<EditText> {

    override fun applyColor(view: EditText, color: Int) {
        if (view is TintableBackgroundView) {
            view.supportBackgroundTintList = Utils.colorToStateList(color)
        }
    }

    override fun getColor(view: EditText): Int {
        if (view is TintableBackgroundView) {
            return view.supportBackgroundTintList?.defaultColor ?: Color.TRANSPARENT
        }

        return Color.TRANSPARENT
    }
}

class EditTextHintAdapter: ColorAdapter<EditText> {

    override fun applyColor(view: EditText, color: Int) {
        view.setHintTextColor(color)
    }

    override fun getColor(view: EditText): Int {
        return view.currentHintTextColor
    }
}

/**
 * Don't make background transparent!
 */
class BackgroundNoAlphaAdapter: ColorAdapter<View> {

    override fun applyColor(view: View, color: Int) {
        when {
            Color.alpha(color) < 255 -> view.setBackgroundColor(ColorUtils.setAlphaComponent(color, 255))
            else -> view.setBackgroundColor(color)
        }
    }

    override fun getColor(view: View): Int {
        val bg = view.background
        return (bg as? ColorDrawable)?.color ?: Color.TRANSPARENT
    }
}

class TabLayoutTextAdapter: ColorAdapter<TabLayout> {

    override fun applyColor(view: TabLayout, color: Int) {
        view.tabTextColors = ColorStateList(
                arrayOf(
                        intArrayOf(-android.R.attr.state_selected),
                        intArrayOf()
                ),
                intArrayOf(
                        ColorUtils.setAlphaComponent(color, 127),
                        color
                )
        )
    }

    override fun getColor(view: TabLayout): Int {
        return view.tabTextColors?.defaultColor ?: Color.TRANSPARENT
    }
}

/**
 * Current tab selection line adapter
 */
class TabLayoutLineAdapter: ColorAdapter<TabLayout> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: TabLayout, color: Int) {
        this.color = color
        view.setSelectedTabIndicatorColor(color)
    }

    override fun getColor(view: TabLayout): Int {
        // can't retrieve it without resorting to reflection, ugh
        return color
    }
}