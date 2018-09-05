package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.io.Serializable


@JsonApi(type = "designs", policy = Policy.DESERIALIZATION_ONLY)
class DesignResponse: Resource() {

        /**
         * Actual nested design data
         */
        @field:Json(name = "data")
        lateinit var data: DesignStyle
}

/**
 * Example:
 * ```
 * {
 *     "background": false,
 *     "name": "Sample",
 *     "colors": {...},
 *     "fonts": {...},
 *     "header": {...},
 *     "layout": {...}
 * }
 * ```
 *
 * @see DesignLayout
 * @see DesignColors
 * @see DesignHeader
 */
data class DesignStyle(
        val name: String?,
        //val background: Boolean?,
        val layout: DesignLayout?,
        val colors: DesignColors?,
        val header: DesignHeader?,
        val fonts: Map<String, DesignFont> // types: entries, elements, headers, menu
) : Serializable

/**
 * Example:
 * ```
 * "layout": {
 *     "align": "left,
 *     "blocks": true,
 *     "epigraph": true,
 *     "layout": 1,
 *     "maxWidth": 1244
 * }
 * ```
 */
data class DesignLayout(
        val align: String?, // left, center, right
        val blocks: Boolean?,
        val epigraph: Boolean?,
        val layout: Int?, // 1 - one column, 2 - two column left menu,  3 - two column right menu
        val maxWidth: Int?
) : Serializable

/**
 * Example:
 * ```
 * "colors": {
 *     "actions": "rgba(81,132,115,1)",
 *     "background": "rgba(222,222,222,1)",
 *     "blocks": "rgba(250,250,250,1)",
 *     "borders": "rgba(198,198,198,1)",
 *     "buttonText": "rgba(255,255,255,1)",
 *     "elements": "rgba(146,146,146,1)",
 *     "elementsBack": "rgba(238,238,238,1)",
 *     "headers": "rgba(51,51,51,1)",
 *     "important": "rgba(222, 65, 58, 1)",
 *     "links": "rgba(81,132,115,1)",
 *     "meta": "rgba(146,146,146,1)",
 *     "text": "rgba(51,51,51,1)"
 * }
 * ```
 */
data class DesignColors(
        val accent: String?,

        val actions: String?,

        val background: String?,

        val blocks: String?,

        val borders: String?,

        @field:Json(name = "button-text")
        val buttonText: String?,

        val buttons: String?,

        val dividers: String?,

        val elements: String?,

        @field:Json(name = "elements-back")
        val elementsBack: String?,

        val headers: String?,

        val important: String?,

        val links: String?,

        val meta: String?,

        val offtop: String?,

        val text: String?
) : Serializable

/**
 * Example:
 * ```
 * "entry": {
 *     "align": "justify",
 *     "family": "'Roboto', 'Helvetica', 'Arial', sans-serif",
 *     "height": "1.45",
 *     "size": "15px",
 *     "style": "normal",
 *     "weight": "normal"
 * }
 * ```
 */
data class DesignFont(
        val family: String?,
        val size: String?,
        val style: String?,
        val weight: String?
) : Serializable

/**
 * Example:
 * ```
 * "header": {
 *     "background": "rgba(222,222,222,1)",
 *     "color": "rgba(255,255,255,1)",
 *     "font": {...},
 *     "image": {...}
 * }
 * ```
 */
data class DesignHeader(
        val background: String?,
        val color: String?,
        val font: DesignFont?,
        val image: DesignImage?
) : Serializable

/**
 * Example:
 * ```
 * "image": {
 *     "attachment": "fixed",
 *     "position": "bottom",
 *     "repeat": "repeat-y",
 *     "size": "cover",
 *     "url": "/static/media/blog_header_bg_full.970626c8.jpg"
 * }
 * ```
 */
data class DesignImage(
        val url: String?,
        val size: String?,
        val repeat: String?,
        val position: String?,
        val attachment: String?
) : Serializable

typealias Design = DesignResponse