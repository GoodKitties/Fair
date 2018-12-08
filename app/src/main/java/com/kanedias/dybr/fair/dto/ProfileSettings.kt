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