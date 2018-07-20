package com.kanedias.dybr.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Comment creation request. Text is mandatory and represented in html
 * Example:
 * ```
 * {
 *   "data": {
 *     "type": "comments",
 *     "attributes": {
 *       "content": "<div/>"
 *     },
 *     "relationships": {
 *       "entry": {
 *         "data": {
 *           "type": "entries",
 *           "id": 7
 *         }
 *       },
 *       "profile": {
 *         "data": {
 *           "type": "profiles",
 *           "id": 28
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 * @author Kanedias
 *
 * Created on 14.01.18
 */
@JsonApi(type = "comments", policy = Policy.SERIALIZATION_ONLY)
class CreateCommentRequest: Resource() {

    /**
     * Content of this comment. Represented in HTML format
     */
    lateinit var content: String

    /**
     * Entry for which this comment is being created for
     */
    var entry : HasOne<Entry>? = null

    /**
     * Profile this comment is being created with
     */
    var profile : HasOne<OwnProfile>? = null
}

/**
 * Represents comment in User -> Profile -> Blog -> Entry -> Comment hierarchy.
 * Example:
 * ```
 * {
 *   "data": {
 *     "id": "5",
 *     "type": "comments",
 *     "links": {
 *       "self": "http://www.example.com/v1/comments/5"
 *     },
 *     "attributes": {
 *       "created-at": "2018-01-09T23:10:59.115Z",
 *       "updated-at": "2018-01-09T23:10:59.115Z",
 *       "content": {
 *         "blocks": [],
 *         "entityMap": {}
 *       }
 *     },
 *     "relationships": {
 *       "blog": {
 *         "links": {
 *           "self": "http://www.example.com/v1/comments/5/relationships/blog",
 *           "related": "http://www.example.com/v1/comments/5/blog"
 *         }
 *       },
 *       "entry": {
 *         "links": {
 *           "self": "http://www.example.com/v1/comments/5/relationships/entry",
 *           "related": "http://www.example.com/v1/comments/5/entry"
 *         }
 *       },
 *       "profile": {
 *         "links": {
 *           "self": "http://www.example.com/v1/comments/5/relationships/profile",
 *           "related": "http://www.example.com/v1/comments/5/profile"
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 14.01.18
 */
@JsonApi(type = "comments", policy = Policy.DESERIALIZATION_ONLY)
class CommentResponse: Resource() {

    /**
     * Text of this comment in HTML format
     */
    lateinit var content: String

    /**
     * Date this comment was created at.
     * Immutable
     */
    @field:Json(name = "created-at")
    lateinit var createdAt: Date

    /**
     * Date this comment was last modified at
     */
    @field:Json(name = "updated-at")
    lateinit var updatedAt: Date

    /**
     * Blog this comment was posted in
     */
    val blog = HasOne<Blog>()

    /**
     * Entry this comment was posted for
     */
    val entry = HasOne<Entry>()

    /**
     * Profile this comment was posted by
     */
    val profile = HasOne<OwnProfile>()
}

typealias Comment = CommentResponse