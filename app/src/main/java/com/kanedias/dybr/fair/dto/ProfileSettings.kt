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
 *     "privacy": {...},
 *     "permissions": {...},
 *     "pinned-entries": ["1", "2", "3"]
 * }
 * ```
 *
 * @see NotificationsSettings
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
        var notifications: NotificationsSettings = NotificationsSettings(),

        @field:Json(name = "privacy")
        var privacy: PrivacySettings = PrivacySettings(),

        @field:Json(name = "permissions")
        var permissions: RecordPermissions = RecordPermissions(),

        @field:Json(name = "pinned-entries")
        var pinnedEntries: MutableSet<String> = mutableSetOf(),

        @field:Json(name = "reactions")
        var reactions: ReactionConfig = ReactionConfig()
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
data class NotificationsSettings(
        var comments: NotificationConfig = NotificationConfig(),
        var entries: NotificationConfig = NotificationConfig()
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
        val enable: Boolean = true,
        val regularity: String = "timely"
) : Serializable

/**
 * Example:
 * ```
 * "reactions": {
 *     "disable": false,
 *     "use-images": false,
 *     "disable-in-blog": false
 * }
 * ```
 */
data class ReactionConfig(
        @field:Json(name = "disable")
        var disable: Boolean = false,

        @field:Json(name = "use-images")
        var useImages: Boolean = false,

        @field:Json(name = "disable-in-blog")
        var disableInBlog: Boolean = false
) : Serializable

/**
 * Pagination is a setting of a user, not profile.
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
        val blogs: Int = 20,
        val comments: Int = 20,
        val entries: Int = 10,
        val profiles: Int = 20
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
        val dybrfeed: Boolean = true
) : Serializable