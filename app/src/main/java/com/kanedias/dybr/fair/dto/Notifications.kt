package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 * Request to create notification. Not used as-is as notifications are usually created on server,
 * from the client side only PATCH requests updating the state will follow.
 */
@JsonApi(type = "notifications", policy = Policy.DESERIALIZATION_ONLY)
class NotificationRequest : Resource() {

    @field:Json(name = "state")
    lateinit var state: String // can be "new" or "read"

    @field:Json(name = "blog")
    var blog: HasOne<Blog>? = null

    @field:Json(name = "entry")
    var entry: HasOne<Entry>? = null

    @field:Json(name = "comment")
    var comment: HasOne<Comment>? = null

    /**
     * The profile and blog of the comment author
     */
    @field:Json(name = "profile")
    var profile: HasOne<OwnProfile>? = null

    /**
     * The profile and blog where the notification originates from
     * i.e. where the comment was written
     */
    @field:Json(name = "source")
    var source: HasOne<OwnProfile>? = null
}

/**
 * @author Kanedias
 *
 * Example:
 * ```
 * {
 *     "id": "120",
 *     "type": "notifications",
 *     "attributes": {
 *         "blog": "34",
 *         "comment": "991",
 *         "entry": "1147",
 *         "state": "new"
 *     },
 *     "relationships": {
 *         "comments": {
 *             "data": {
 *               "id": "991",
 *               "type": "comments"
 *             }
 *         },
 *         "profiles": {
 *             "data": {
 *                 "id": "136",
 *                 "type": "profiles"
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * Created on 23.09.18
 */
@JsonApi(type = "notifications", policy = Policy.DESERIALIZATION_ONLY)
class NotificationResponse : Resource() {

    /**
     * State of this notification. Can be "new" or "read"
     */
    @field:Json(name = "state")
    lateinit var state: String

    /**
     * Id of the entry this notification is caused by
     */
    @field:Json(name = "entry")
    lateinit var entryId: String

    /**
     * Id of the blog this notification originates from
     */
    @field:Json(name = "blog")
    lateinit var blogId: String

    /**
     * Comment that caused this notification
     */
    @field:Json(name = "comments")
    var comment = HasOne<Comment>()

    /**
     * Profile of the comment author
     */
    @field:Json(name = "profiles")
    var profile = HasOne<OwnProfile>()

    /**
     * The profile and blog where the notification originates from
     * i.e. where the comment was written
     */
    @field:Json(name = "source")
    var source = HasOne<OwnProfile>()
}

typealias Notification = NotificationResponse