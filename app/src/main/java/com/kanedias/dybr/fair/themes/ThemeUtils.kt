package com.kanedias.dybr.fair.themes

import android.content.Context
import android.graphics.Color
import android.support.v4.graphics.ColorUtils
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
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
const val BACKGROUND = 3
const val TEXT_BLOCK = 4
const val TEXT = 5

/**
 * Converts rgba string to standard color integer notation
 * @param rgba css string in the form of `rgba(255, 127, 63, 255)`
 */
infix fun String.colorFromCss(toDark: Boolean) : Int? {
    val rgbaCssString = Pattern.compile("rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)")
    val searcher = rgbaCssString.matcher(this)
    if (!searcher.find()) {
        return null
    }
    val rr = searcher.group(1).toInt()
    val gg = searcher.group(2).toInt()
    val bb = searcher.group(3).toInt()
    val aa = (searcher.group(4).toFloat() * 255).toInt()
    val candidate =  Color.argb(aa, rr, gg, bb)
    if (toDark && ColorUtils.calculateLuminance(candidate) > 0.3) {
        return ColorUtils.blendARGB(candidate, Color.BLACK, 0.5f)
    }

    return candidate
}

fun applyTheme(blog: Blog?, target: Any) {
    if (isMarkerBlog(blog))
        return

    if (blog?.slug.isNullOrEmpty())
        return

    launch(UI) {
        val fullBlog = async { Network.loadBlog(blog?.slug!!) }.await()
        val relatedProf = fullBlog.profile.get(fullBlog.document) ?: return@launch
        val currentDesign = relatedProf.settings?.currentDesign ?: return@launch
        val design: DesignStyle = relatedProf.settings?.designs?.get(currentDesign) ?: return@launch

        design.colors?.headers?.colorFromCss(toDark = true)?.let {
            // use for toolbar color
            Scoop.getInstance().update(PRIMARY, it)
            Scoop.getInstance().update(PRIMARY_DARK, ColorUtils.blendARGB(it, Color.BLACK, 0.2f))
        }

        design.colors?.background?.colorFromCss(toDark = true)?.let { Scoop.getInstance().update(BACKGROUND, it) }
        design.colors?.blocks?.colorFromCss(toDark = true)?.let { Scoop.getInstance().update(TEXT_BLOCK, it) }
        design.colors?.text?.colorFromCss(toDark = true)?.let { Scoop.getInstance().update(TEXT, it) }
        design.colors?.actions?.colorFromCss(toDark = true)?.let { Scoop.getInstance().update(ACCENT, it) }
    }
}


@Suppress("DEPRECATION") // need to support API < 23
fun resetTheme(ctx: Context) {
    Scoop.getInstance().update(PRIMARY, ctx.resources.getColor(R.color.colorPrimary))
    Scoop.getInstance().update(PRIMARY_DARK, ctx.resources.getColor(R.color.colorPrimaryDark))
    Scoop.getInstance().update(ACCENT, ctx.resources.getColor(R.color.colorAccent))
}