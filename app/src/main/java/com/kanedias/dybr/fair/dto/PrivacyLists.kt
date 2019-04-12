package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasMany
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.io.Serializable

/**
 * Example:
 * ```
 * {
 *     "scope": "feed",
 *     "action": "hide",
 *     "entries": null,
 *     "profiles": [
 *         844
 *     ]
 * }
 * ```
 */
@JsonApi(type = "profile-list", policy = Policy.DESERIALIZATION_ONLY)
class PrivacyEntryResponse: Resource(), Serializable {

    @field:Json(name = "action")
    lateinit var action: String

    @field:Json(name = "kind")
    lateinit var kind: String

    @field:Json(name = "name")
    lateinit var name: String

    @field:Json(name = "scope")
    lateinit var scope: String

    @field:Json(name = "profiles")
    var profiles = HasMany<OwnProfile>()
}