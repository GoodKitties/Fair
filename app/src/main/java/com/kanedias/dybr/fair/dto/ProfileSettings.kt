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
        @Json(name = "avatar")
        val avatar: String? = null,

        @Json(name = "subtext")
        val subtext: String? = null,

        @Json(name = "current-design")
        val currentDesign: String? = null,

        @Json(name = "notifications")
        var notifications: NotificationsSettings = NotificationsSettings(),

        @Json(name = "privacy")
        var privacy: PrivacySettings = PrivacySettings(),

        @Json(name = "permissions")
        var permissions: RecordPermissions = RecordPermissions(),

        @Json(name = "pinned-entries")
        var pinnedEntries: MutableSet<String> = mutableSetOf(),

        @Json(name = "reactions")
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
        @Json(name = "hide")
        var disable: Boolean = false,

        @Json(name = "use-images")
        var useImages: Boolean = false,

        @Json(name = "disable-in-blog")
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