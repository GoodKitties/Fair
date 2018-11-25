package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Blog creation request. Requires profile relation, title and slug
 * Example:
 * ```
 * {
 *   "data": {
 *     "type": "blogs",
 *     "attributes": {
 *       "slug": "laboriosam_et",
 *       "title": "Delectus iure voluptatem ut."
 *     },
 *     "relationships": {
 *       "profile": {
 *         "data": {
 *           "type": "profiles",
 *           "id": 8
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
@JsonApi(type = "blogs", policy = Policy.SERIALIZATION_ONLY)
class BlogCreateRequest : Resource() {

    /**
     * Address part of this blog on main website, e.g. https://dybr/ru/__my-precious-blog__
     */
    @field:Json(name = "slug")
    lateinit var slug: String

    /**
     * Title in header of the browser and caption forms
     */
    @field:Json(name = "title")
    lateinit var title: String


    /**
     * Profile for new blog to be linked to
     */
    @field:Json(name = "profile")
    var profile = HasOne<OwnProfile>()
}

/**
 * Represents Blog in User -> Profile -> Blog -> Entry -> Comment relation
 * Example:
 * ```
 * {
 *   "data": {
 *     "id": "6",
 *     "type": "blogs",
 *     "links": {
 *       "self": "http://www.example.com/v1/blogs/6"
 *     },
 *     "attributes": {
 *       "created-at": "2018-01-09T23:10:56.737Z",
 *       "updated-at": "2018-01-09T23:10:56.737Z",
 *       "slug": "dolorem-voluptatibus",
 *       "title": "Maxime non vel sed et."
 *     },
 *     "relationships": {
 *       "profile": {
 *         "links": {
 *           "self": "http://www.example.com/v1/blogs/6/relationships/profile",
 *           "related": "http://www.example.com/v1/blogs/6/profile"
 *         }
 *       },
 *       "comments": {
 *         "links": {
 *           "self": "http://www.example.com/v1/blogs/6/relationships/comments",
 *           "related": "http://www.example.com/v1/blogs/6/comments"
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
@JsonApi(type = "blogs", policy = Policy.DESERIALIZATION_ONLY)
class BlogResponse : Resource() {

    /**
     * See [BlogCreateRequest.slug]
     */
    @field:Json(name = "slug")
    lateinit var slug: String

    /**
     * See [BlogCreateRequest.title]
     */
    @field:Json(name = "title")
    lateinit var title: String

    /**
     * Date this blog was created at.
     * Immutable
     */
    @field:Json(name = "created-at")
    lateinit var createdAt: Date

    /**
     * Date this blog was last modified at
     */
    @field:Json(name = "updated-at")
    lateinit var updatedAt: Date

    /**
     * Profile this blog belongs to
     */
    @field:Json(name = "profile")
    var profile = HasOne<OwnProfile>()

    /**
     * Comments related to this blog. This is link relation, thus One-to-One.
     */
    @field:Json(name = "comments")
    var comments = HasOne<Comment>()
}

fun isBlogWritable(profile: OwnProfile?) = profile == Auth.profile
fun isMarkerBlog(profile: OwnProfile?) = profile == Auth.favoritesMarker || profile == Auth.worldMarker

typealias Blog = BlogResponse