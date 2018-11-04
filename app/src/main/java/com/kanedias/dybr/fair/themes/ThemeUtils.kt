package com.kanedias.dybr.fair.themes

import android.content.Context
import android.graphics.Color
import android.preference.PreferenceManager
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.v4.graphics.ColorUtils
import android.util.Log
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.dto.Blog
import com.kanedias.dybr.fair.dto.Design
import com.kanedias.dybr.fair.dto.isMarkerBlog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.regex.Pattern

const val TOOLBAR = 0       // top toolbar color
const val STATUS_BAR = 1    // status bar color (a thin bar above toolbar)
const val TOOLBAR_TEXT = 2  // toolbar title text
const val ACCENT = 3        // floating button color
const val BACKGROUND = 4    // background for main view
const val DIVIDER = 5       // color of divider lines (between text and edit/delete buttons)
const val TEXT_BLOCK = 6    // background for text cards
const val TEXT = 7          // color of main text
const val TEXT_HEADERS = 8  // color of entry titles
const val TEXT_LINKS = 9    // color of links in text
const val TEXT_OFFTOP = 10  // color of offtop and hints in text

/**
 * Converts rgba string to standard color integer notation
 * String should be css string in the form of `rgba(255, 127, 63, 0.39)`
 */
fun String.colorFromCss() : Int? {
    val rgbaCssString = Pattern.compile("rgba\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([\\d.]+)\\s*\\)")
    val searcher = rgbaCssString.matcher(this)
    if (!searcher.find()) {
        return null
    }
    val rr = searcher.group(1).toInt()
    val gg = searcher.group(2).toInt()
    val bb = searcher.group(3).toInt()
    val aa = (searcher.group(4).toFloat() * 255).toInt()
    return Color.argb(aa, rr, gg, bb)
}

/**
 * Same as [ColorUtils.blendARGB] but leave alpha as in [color1]
 */
@ColorInt
fun blendRGB(@ColorInt color1: Int, @ColorInt color2: Int, @FloatRange(from = 0.0, to = 1.0) ratio: Float): Int {
    val inverseRatio = 1 - ratio
    val a = Color.alpha(color1) // we don't touch alpha channel
    val r = Color.red(color1) * inverseRatio + Color.red(color2) * ratio
    val g = Color.green(color1) * inverseRatio + Color.green(color2) * ratio
    val b = Color.blue(color1) * inverseRatio + Color.blue(color2) * ratio
    return Color.argb(a, r.toInt(), g.toInt(), b.toInt())
}

/**
 * Apply theme to the blog if it has one. Ignored if not checked in preferences.
 *
 * Note: Requires network. Resolves blog -> profile -> current profile design.
 *
 * @param blog blog to retrieve themes for
 * @param target target context where theme is applied
 */
fun applyTheme(blog: Blog, target: Context) {
    // don't apply to service blogs e.g. to favorites or world
    if (isMarkerBlog(blog))
        return

    // don't apply if user explicitly shat it down
    val prefs = PreferenceManager.getDefaultSharedPreferences(target)
    if (!prefs.getBoolean("apply-blog-theme", true))
        return

    launch(Dispatchers.Main) {
        try {
            val fullBlog = async(Dispatchers.IO) { Network.loadBlog(blog.id) }.await()
            val relatedProf = fullBlog.profile.get(fullBlog.document) ?: return@launch
            val design = async(Dispatchers.IO) { Network.loadProfileDesign(relatedProf) }.await() ?: return@launch

            updateColorBindings(design)
        } catch (ex: Exception) {
            // do nothing - themes are optional
            Log.e("ThemeEngine", "Couldn't apply theme", ex)
        }
    }
}

fun updateColorBindings(design: Design) {
    // toolbar colors
    design.data.header?.let {
        val tbBackground = it.background?.colorFromCss() ?: return@let
        var tbText = it.color?.colorFromCss() ?: return@let

        // workaround: if alpha is lower than 0.5 do nothing
        if (Color.alpha(tbBackground) < 127) {
            return@let
        }

        // workaround: if contrast between toolbar and text is lower than 7
        // make text more distinctive, see https://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef
        if (ColorUtils.calculateContrast(tbText, tbBackground) < 7) {
            val luminance = ColorUtils.calculateLuminance(tbBackground)
            tbText = when {
                luminance < 0.5 -> ColorUtils.blendARGB(tbText, Color.WHITE, 0.5f)
                else -> ColorUtils.blendARGB(tbText, Color.BLACK, 0.5f)
            }
        }

        Scoop.getInstance().update(TOOLBAR, tbBackground)
        Scoop.getInstance().update(STATUS_BAR, blendRGB(tbBackground, Color.BLACK, 0.2f))
        Scoop.getInstance().update(TOOLBAR_TEXT, tbText)
    }

    // common background color
    design.data.colors?.background?.colorFromCss()?.let {
        // workaround: if alpha is lower than 1 blend with white
        // because default website color is white
        val alpha = Color.alpha(it).toFloat() / 255
        if (alpha < 0.5) {
            Scoop.getInstance().update(BACKGROUND, ColorUtils.compositeColors(it, Color.WHITE))
            return@let
        }

        Scoop.getInstance().update(BACKGROUND, it)
    }

    // color of text cards
    design.data.colors?.blocks?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_BLOCK, it) }

    // color of text on the cards
    design.data.colors?.text?.colorFromCss()?.let { Scoop.getInstance().update(TEXT, it) }

    // color of offtop and hints in text
    design.data.colors?.offtop?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_OFFTOP, it) }

    // color of entry titles on the cards
    design.data.colors?.headers?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_HEADERS, it) }

    // dividers between entry text and buttons
    design.data.colors?.dividers?.colorFromCss()?.let { Scoop.getInstance().update(DIVIDER, it) }

    // color of links like "edit" or "delete"
    design.data.colors?.links?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_LINKS, it) }

    // accent color
    design.data.colors?.accent?.colorFromCss()?.let { Scoop.getInstance().update(ACCENT, it) }
}