package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 * Bookmark creation request. Linked entry is mandatory.
 * Example:
 * ```
 * {
 *   "data": {
 *     "type": "bookmarks",
 *     "attributes": {},
 *     "relationships": {
 *       "entry": {
 *         "data": {
 *           "id": "21835",
 *           "type": "entries"
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 24.03.19
 */
@JsonApi(type = "bookmarks", policy = Policy.SERIALIZATION_ONLY)
class CreateBookmarkRequest: Resource() {

    /**
     * Entry this bookmark points to.
     * Must be set.
     */
    @field:Json(name = "entry")
    lateinit var entry: HasOne<Entry>

}

/**
 * Bookmark creation response
 *
 * @author Kanedias
 *
 * Created on 24.03.19
 */
@JsonApi(type = "bookmarks", policy = Policy.DESERIALIZATION_ONLY)
class CreateBookmarkResponse: Resource() {

    /**
     * Entry this bookmark points to.
     * Must be set.
     */
    var entry: HasOne<Entry>? = null
}

typealias Bookmark = CreateBookmarkResponse