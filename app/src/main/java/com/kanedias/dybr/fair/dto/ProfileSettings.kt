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
 *     "privacy": {...},
 *     "permissions": {...},
 *     "pinned-entries": ["1", "2", "3"]
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
        val avatar: String? = null,

        @field:Json(name = "subtext")
        val subtext: String? = null,

        @field:Json(name = "current-design")
        val currentDesign: String? = null,

        @field:Json(name = "notifications")
        val notifications: NotificationSettings? = null,

        @field:Json(name = "pagination")
        val pagination: PaginationSettings? = null,

        @field:Json(name = "privacy")
        val privacy: PrivacySettings? = null,

        @field:Json(name = "permissions")
        val permissions: RecordPermissions? = null,

        @field:Json(name = "pinned-entries")
        val pinnedEntries: MutableSet<String>? = null
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
        val comments: NotificationConfig? = null,
        val entries: NotificationConfig? = null
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
        val enable: Boolean? = null,
        val regularity: String? = null
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
        val blogs: Int? = null,
        val comments: Int? = null,
        val entries: Int? = null,
        val profiles: Int? = null
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
        val dybrfeed: Boolean? = null
) : Serializable