package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 *
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

    @field:Json(name = "profile")
    var profile: HasOne<OwnProfile>? = null
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

    @field:Json(name = "state")
    lateinit var state: String // can be "new" or "read"

    @field:Json(name = "blog")
    var blog = HasOne<Blog>()

    @field:Json(name = "entry")
    var entry = HasOne<Entry>()

    @field:Json(name = "comments")
    var comment = HasOne<Comment>()

    @field:Json(name = "profiles")
    var profile = HasOne<OwnProfile>()
}

typealias Notification = NotificationResponse