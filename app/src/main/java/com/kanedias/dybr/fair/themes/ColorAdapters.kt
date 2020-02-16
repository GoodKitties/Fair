package com.kanedias.dybr.fair.themes

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.TransitionDrawable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.TintableBackgroundView
import androidx.core.widget.CompoundButtonCompat
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.AppCompatSpinner
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.widget.*
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.MenuItemCompat
import androidx.core.widget.TextViewCompat
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeTransition
import com.bumptech.glide.request.transition.Transition
import com.ftinc.scoop.adapters.ColorAdapter
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

class BackgroundDelayedAdapter(private val url: String): ColorAdapter<View> {

    override fun getColor(view: View): Int {
        return Color.TRANSPARENT
    }

    override fun applyColor(view: View, @ColorInt color: Int) {
        // don't apply if we're already in progress or have bg set
        if (url.isBlank() || view.background is BitmapDrawable || view.background is TransitionDrawable)
            return

        val resolved = Network.resolve(url) ?: return

        Glide.with(view)
                .load(resolved.toString())
                .apply(RequestOptions()
                        .override(view.width, view.height)
                        .centerCrop())
                .into(BgDrawableTarget(view))
    }

    inner class BgDrawableTarget(view: View): CustomViewTarget<View, Drawable>(view) {

        private val cf = DrawableCrossFadeTransition(300, true)

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
            GlobalScope.launch(Dispatchers.Main) {
                delay(600)
                cf.transition(resource, BgAdapter(view))
            }
        }

        override fun onLoadFailed(errorDrawable: Drawable?) {
        }

        override fun onResourceCleared(placeholder: Drawable?) {
            view.background = null
        }
    }

    inner class BgAdapter(private val view: View) : Transition.ViewAdapter {

        override fun getView() = view

        override fun getCurrentDrawable(): Drawable? = view.background

        override fun setDrawable(drawable: Drawable) {
            view.background = drawable
        }

    }
}

class BackgroundTintColorAdapter : ColorAdapter<View> {

    override fun applyColor(view: View, @ColorInt color: Int) {
        val colorStateList = ColorStateList.valueOf(color)
        view.backgroundTintList = colorStateList
    }

    override fun getColor(view: View): Int {
        val tintList = view.backgroundTintList ?: return Color.TRANSPARENT
        return tintList.defaultColor
    }
}

class FabIconAdapter : ColorAdapter<FloatingActionButton> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: FloatingActionButton, @ColorInt color: Int) {
        this.color = color

        DrawableCompat.setTint(view.drawable, color)
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

class ToolbarIconsAdapter : ColorAdapter<Toolbar> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: Toolbar, @ColorInt color: Int) {
        this.color = color

        for (drawable in listOfNotNull(view.navigationIcon, view.overflowIcon, view.collapseIcon)) {
            // drawer arrow drawable can't be tinted
            if (drawable is DrawerArrowDrawable) {
                drawable.color = color
                continue
            }

            DrawableCompat.setTint(drawable, color)
        }
    }

    override fun getColor(view: Toolbar): Int {
        return color
    }
}

class ToolbarMenuIconsAdapter : ColorAdapter<Toolbar> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: Toolbar, @ColorInt color: Int) {
        this.color = color

        for (idx in 0 until view.menu.size()) {
            val mi = view.menu.getItem(idx)
            MenuItemCompat.setIconTintList(mi, ColorStateList.valueOf(color))
        }
    }

    override fun getColor(view: Toolbar): Int {
        return color
    }
}

class SearchIconsAdapter : ColorAdapter<SearchView> {

    private  fun getDrawable(content: View, name: String) : Drawable? {
        val field = content::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field.get(content) as? Drawable?
    }

    private var color = Color.TRANSPARENT

    override fun applyColor(view: SearchView, @ColorInt color: Int) {
        this.color = color

        val searchImg = view.findViewById<ImageView>(androidx.appcompat.R.id.search_button)
        val collImg = view.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
        val goImg = view.findViewById<ImageView>(androidx.appcompat.R.id.search_go_btn)
        val closeImg = view.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val voiceImg = view.findViewById<ImageView>(androidx.appcompat.R.id.search_voice_btn)

        for (image in listOfNotNull(searchImg, collImg, goImg, closeImg, voiceImg)) {
            DrawableCompat.setTint(image.drawable, color)
        }

        getDrawable(view, "mSearchHintIcon")?.let {
            val hintColor = ColorUtils.setAlphaComponent(color, 127)
            DrawableCompat.setTint(it, hintColor)
        }

    }

    override fun getColor(view: SearchView): Int {
        return color
    }
}

class SearchTextAdapter : ColorAdapter<SearchView> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: SearchView, @ColorInt color: Int) {
        this.color = color

        val textView = view.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
        textView.setTextColor(color)

        val hintColor = ColorUtils.setAlphaComponent(color, 127)
        textView.setHintTextColor(hintColor)
    }

    override fun getColor(view: SearchView): Int {
        return color
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

/**
 * Same as [TextViewColorAdapter] but with disabled color support
 */
class TextViewDisableAwareColorAdapter : ColorAdapter<TextView> {

    override fun applyColor(view: TextView, @ColorInt color: Int) {
        val stateList = ColorStateList(
                arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                ),
                intArrayOf(
                        ColorUtils.setAlphaComponent(color, 127),
                        color
                )
        )
        view.setTextColor(stateList)
    }

    override fun getColor(view: TextView): Int {
        return view.currentTextColor
    }
}


class TextViewDrawableAdapter: ColorAdapter<TextView> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: TextView, color: Int) {
        this.color = color

        for (drawable in view.compoundDrawables.filterNotNull()) {
            DrawableCompat.setTint(drawable, color)
        }
    }

    override fun getColor(view: TextView): Int {
        return color
    }
}

class CheckBoxAdapter: ColorAdapter<CheckBox> {

    override fun applyColor(view: CheckBox, color: Int) {
        CompoundButtonCompat.setButtonTintList(view, ColorStateList.valueOf(color))
    }

    override fun getColor(view: CheckBox): Int {
        return CompoundButtonCompat.getButtonTintList(view)?.defaultColor ?: Color.TRANSPARENT
    }

}

class EditTextAdapter: ColorAdapter<EditText> {

    override fun applyColor(view: EditText, color: Int) {
        view.setTextColor(color)
        view.setHintTextColor(ColorUtils.setAlphaComponent(color, 127))
    }

    override fun getColor(view: EditText): Int {
        return view.textColors.defaultColor
    }
}

class EditTextLineAdapter: ColorAdapter<EditText> {

    override fun applyColor(view: EditText, color: Int) {
        if (view is TintableBackgroundView) {
            view.supportBackgroundTintList = ColorStateList.valueOf(color)
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

class SwitchDrawableColorAdapter: ColorAdapter<SwitchCompat> {

    override fun applyColor(view: SwitchCompat, color: Int) {
        TextViewCompat.setCompoundDrawableTintList(view,
            ColorStateList(
                arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(-android.R.attr.state_checked),
                        intArrayOf()
                ),
                intArrayOf(
                        ColorUtils.setAlphaComponent(color, 127),
                        ColorUtils.blendARGB(color, Color.BLACK, 0.5f),
                        color
                )
            )
        )
    }

    override fun getColor(view: SwitchCompat): Int {
        return TextViewCompat.getCompoundDrawableTintList(view)?.defaultColor ?: Color.TRANSPARENT
    }
}

class SwitchColorThumbAdapter: ColorAdapter<SwitchCompat> {

    override fun applyColor(view: SwitchCompat, color: Int) {
        view.thumbTintList = ColorStateList(
                arrayOf(
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf(-android.R.attr.state_checked),
                        intArrayOf()
                ),
                intArrayOf(
                        ColorUtils.setAlphaComponent(color, 127),
                        ColorUtils.blendARGB(color, Color.BLACK, 0.5f),
                        color
                )
        )
    }

    override fun getColor(view: SwitchCompat): Int {
        return view.thumbTintList?.defaultColor ?: Color.TRANSPARENT
    }
}

class SwitchColorAdapter: ColorAdapter<SwitchCompat> {

    override fun applyColor(view: SwitchCompat, color: Int) {
        view.trackTintList = ColorStateList(
                arrayOf(
                        intArrayOf(-android.R.attr.state_checked),
                        intArrayOf(-android.R.attr.state_enabled),
                        intArrayOf()
                ),
                intArrayOf(
                        ColorUtils.blendARGB(color, Color.BLACK, 0.5f),
                        ColorUtils.blendARGB(color, Color.BLACK, 0.5f),
                        color
                )
        )
    }

    override fun getColor(view: SwitchCompat): Int {
        return view.trackTintList?.defaultColor ?: Color.TRANSPARENT
    }
}

/**
 * Material dialog button gains color on measure, we have to delay our own color set
 */
class MaterialDialogButtonAdapter: ColorAdapter<DialogActionButton> {

    override fun applyColor(view: DialogActionButton, color: Int) {
        // TODO: hack, better make animation duration more usable in scoop
        view.postDelayed({ view.updateTextColor(color) }, 100)
        view.updateTextColor(color)
    }

    override fun getColor(view: DialogActionButton) = view.textColors.defaultColor
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

class SpinnerDropdownColorAdapter: ColorAdapter<AppCompatSpinner> {

    private var color = Color.TRANSPARENT

    override fun applyColor(view: AppCompatSpinner, color: Int) {
        this.color = color

        DrawableCompat.setTint(view.background, color)
    }

    override fun getColor(view: AppCompatSpinner): Int {
        return color
    }

}