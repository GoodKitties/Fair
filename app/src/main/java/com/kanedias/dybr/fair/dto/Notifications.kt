package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

@JsonApi(type = "notifications", policy = Policy.DESERIALIZATION_ONLY)
class NotificationRequest : Resource() {

    @field:Json(name = "state")
    lateinit var state: String // can be "new" or "read"

    @field:Json(name = "blog")
    var blog: HasOne<Blog>? = null

    @field:Json(name = "entry")
    var entry: HasOne<Entry>? = null

    @field:Json(name = "profile")
    var profile: HasOne<OwnProfile>? = null
}

/**
 * @author Kanedias
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

    @field:Json(name = "profile")
    var profile = HasOne<OwnProfile>()
}

typealias Notification = NotificationResponse