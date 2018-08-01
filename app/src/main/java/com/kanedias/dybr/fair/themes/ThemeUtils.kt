package com.kanedias.dybr.fair.themes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.annotation.ColorInt
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.entities.Blog
import com.kanedias.dybr.fair.entities.DesignStyle
import com.kanedias.dybr.fair.entities.isMarkerBlog
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.regex.Pattern

const val PRIMARY = 0
const val PRIMARY_DARK = 1
const val ACCENT = 2

/**
 * Converts rgba string to standard color integer notation
 * @param rgba css string in the form of `rgba(255, 127, 63, 255)`
 */
fun colorFromCss(rgba: String) : Int? {
    val rgbaCssString = Pattern.compile("rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
    val searcher = rgbaCssString.matcher(rgba)
    if (searcher.find()) {
        val rr = searcher.group(1).toInt()
        val gg = searcher.group(2).toInt()
        val bb = searcher.group(3).toInt()
        val aa = (searcher.group(4).toFloat() * 255).toInt()
        return Color.argb(aa, rr, gg, bb)
    }

    return null
}

fun colorToStateList(@ColorInt color: Int): ColorStateList {
    return ColorStateList(arrayOf(IntArray(0)), intArrayOf(color))
}

fun colorToStateList(@ColorInt color: Int, @ColorInt disabledColor: Int): ColorStateList {
    return ColorStateList(
            arrayOf(
                    intArrayOf(-android.R.attr.state_enabled),
                    intArrayOf(-android.R.attr.state_checked),
                    intArrayOf()),
            intArrayOf(
                    disabledColor,
                    disabledColor,
                    color)
    )
}

fun applyTheme(blog: Blog?, target: Any) {
    if (isMarkerBlog(blog))
        return

    if (blog?.slug.isNullOrEmpty())
        return

    launch(UI) {
        val fullBlog = async { Network.loadBlog(blog?.slug!!) }.await()
        val relatedProf = fullBlog.profile.get(fullBlog.document) ?: return@launch
        //val currentDesign = relatedProf.settings?.currentDesign ?: return@launch
        //val design: DesignStyle = relatedProf.settings?.designs?.get(currentDesign) ?: return@launch
        val design: DesignStyle = relatedProf.settings?.designs?.entries?.last()?.value ?: return@launch

        //design.colors?.background?.let { colorFromCss(it)?.let { Scoop.getInstance().update(target, PRIMARY, it) } }
        //design.colors?.borders?.let { colorFromCss(it)?.let { Scoop.getInstance().update(target, PRIMARY_DARK, it) } }
        //design.colors?.actions?.let { colorFromCss(it)?.let { Scoop.getInstance().update(target, ACCENT, it) } }
    }
}


@Suppress("DEPRECATION") // need to support API < 23
fun resetTheme(ctx: Context, target: Any) {
    //Scoop.getInstance().update(target, PRIMARY, ctx.resources.getColor(R.color.colorPrimary))
    //Scoop.getInstance().update(target, PRIMARY_DARK, ctx.resources.getColor(R.color.colorPrimaryDark))
    //Scoop.getInstance().update(target, ACCENT, ctx.resources.getColor(R.color.colorAccent))
}