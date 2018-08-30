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
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.dto.Blog
import com.kanedias.dybr.fair.dto.isMarkerBlog
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.util.regex.Pattern

const val TOOLBAR = 0
const val STATUS_BAR = 1
const val TOOLBAR_TEXT = 2
const val ACCENT = 3
const val BACKGROUND = 4
const val DIVIDER = 5
const val TEXT_BLOCK = 6
const val TEXT = 7
const val TEXT_HEADERS = 8
const val TEXT_LINKS = 9

/**
 * Converts rgba string to standard color integer notation
 * @param rgba css string in the form of `rgba(255, 127, 63, 255)`
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

fun applyTheme(blog: Blog, target: Context) {
    // don't apply to service blogs e.g. to favorites or world
    if (isMarkerBlog(blog))
        return

    // don't apply if user explicitly shat it down
    val prefs = PreferenceManager.getDefaultSharedPreferences(target)
    if (!prefs.getBoolean("apply-blog-theme", true))
        return

    launch(UI) {
        try {
            val fullBlog = async { Network.loadBlog(blog.id) }.await()
            val relatedProf = fullBlog.profile.get(fullBlog.document) ?: return@launch
            val design = async { Network.loadProfileDesign(relatedProf) }.await() ?: return@launch

            // toolbar color
            var toolbarIsTransparent = false
            design.data.header?.background?.colorFromCss()?.let {
                // workaround: if alpha is lower than 0.5 do nothing
                if (Color.alpha(it) < 127) {
                    toolbarIsTransparent = true
                    return@let
                }

                Scoop.getInstance().update(TOOLBAR, it)
                Scoop.getInstance().update(STATUS_BAR, blendRGB(it, Color.BLACK, 0.2f))
            }

            // toolbar text color
            design.data.header?.color?.colorFromCss()?.let {
                // workaround: if header alpha is lower than 0.5 do nothing
                if (toolbarIsTransparent)
                    return@let

                Scoop.getInstance().update(TOOLBAR_TEXT, it)
            }

            // common background color
            design.data.colors?.background?.colorFromCss()?.let {
                // workaround: if alpha is lower than 1 blend with white
                // because default website color is white
                val alpha = Color.alpha(it).toFloat() / 255
                if (alpha < 0.5) {
                    Scoop.getInstance().update(BACKGROUND, blendRGB(Color.WHITE, it, 1 - alpha))
                    return@let
                }

                Scoop.getInstance().update(BACKGROUND, it)
            }

            // color of text cards
            design.data.colors?.blocks?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_BLOCK, it) }

            // color of text on the cards
            design.data.colors?.text?.colorFromCss()?.let { Scoop.getInstance().update(TEXT, it) }

            // color of entry titles on the cards
            design.data.colors?.headers?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_HEADERS, it) }

            // dividers between entry text and buttons
            design.data.colors?.dividers?.colorFromCss()?.let { Scoop.getInstance().update(DIVIDER, it) }

            // color of links like "edit" or "delete"
            design.data.colors?.links?.colorFromCss()?.let { Scoop.getInstance().update(TEXT_LINKS, it) }

            // accent color
            design.data.colors?.accent?.colorFromCss()?.let { Scoop.getInstance().update(ACCENT, it) }
        } catch (ex: Exception) {
            // do nothing - themes are optional
            Log.e("ThemeEngine", "Couldn't apply theme", ex)
        }
    }
}


@Suppress("DEPRECATION") // need to support API < 23
fun resetTheme(ctx: Context) {
    Scoop.getInstance().update(TOOLBAR, ctx.resources.getColor(R.color.colorPrimary))
    Scoop.getInstance().update(STATUS_BAR, ctx.resources.getColor(R.color.colorPrimaryDark))
    Scoop.getInstance().update(ACCENT, ctx.resources.getColor(R.color.colorAccent))
}