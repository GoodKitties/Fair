package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Main settings structure linked with profile
 * Example:
 * ```
 * "settings": {
 *     "avatar": "https://dybr.ru/img/3/xxxxxxxx.jpg",
 *     "subtext": "Sample text",
 *     "designs": {...},
 *     "notifications": {...},
 *     "pagination": {...},
 *     "privacy": {...}
 * }
 * ```
 *
 * @see NotificationSettings
 * @see PaginationSettings
 * @see PrivacySettings
 *
 * @author Kanedias
 *
 * Created on 28.07.18
 */
data class ProfileSettings(
        @field:Json(name = "avatar")
        val avatar: String?,

        @field:Json(name = "subtext")
        val subtext: String?,

        @field:Json(name = "current-design")
        val currentDesign: String?,

        @field:Json(name = "designs")
        val designs: Map<String, DesignStyle>?,

        @field:Json(name = "notifications")
        val notifications: NotificationSettings?,

        @field:Json(name = "pagination")
        val pagination: PaginationSettings?,

        @field:Json(name = "privacy")
        val privacy: PrivacySettings?
) : Serializable

/**
 * Example:
 * "notifications": {
 *     "comments": {...},
 *     "entries": {...}
 * }
 *
 * @see NotificationConfig
 */
data class NotificationSettings(
        val comments: NotificationConfig?,
        val entries: NotificationConfig?
) : Serializable

/**
 * Example:
 * ```
 * "entries": {
 *     "enable": false,
 *     "regularity": "timely"
 * }
 * ```
 */
data class NotificationConfig(
        val enable: Boolean?,
        val regularity: String?
) : Serializable

/**
 * Example:
 * ```
 * "pagination": {
 *     "blogs": 20,
 *     "comments": 20,
 *     "entries": 10,
 *     "profiles": 20
 * }
 * ```
 */
data class PaginationSettings(
        val blogs: Int?,
        val comments: Int?,
        val entries: Int?,
        val profiles: Int?
) : Serializable

/**
 * Example:
 * ```
 * "privacy": {
 *     "dybrfeed": true
 * }
 * ```
 */
data class PrivacySettings(
        val dybrfeed: Boolean?
) : Serializable

/**
 * Example:
 * ```
 * "1532550279254": {
 *     "id": 1532550279254,
 *     "background": false,
 *     "name": "Sample",
 *     "colors": {...},
 *     "font": {...},
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
        val id: Long?,
        val name: String?,
        //val background: Boolean?,
        val layout: DesignLayout?,
        val colors: DesignColors?,
        val header: DesignHeader?,
        // TODO: font*s*
        val font: Map<String, DesignFont> // types: entries, elements, headers, menu
) : Serializable

/**
 * Example:
 * ```
 * "layout": {
 *     "align": 2,
 *     "blocks": true,
 *     "epigraph": true,
 *     "layout": 1,
 *     "maxWidth": 1244
 * }
 * ```
 */
data class DesignLayout(
        val align: Int?, // 1 - left, 2 - center, 3 - right
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
        val actions: String?,
        val background: String?,
        val blocks: String?,
        val borders: String?,
        val buttonText: String?,
        val elements: String?,
        val elementsBack: String?,
        val headers: String?,
        val important: String?,
        val links: String?,
        val meta: String?,
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