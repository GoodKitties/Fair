package com.kanedias.dybr.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Entry creation request
 * Example:
 * ```
 * {
 *   "data": {
 *     "type": "entries",
 *     "attributes": {
 *       "title": "Repudiandae voluptatem laudantium rerum iure soluta voluptatibus asperiores cupiditate.",
 *       "content": "Iste sint tempore eveniet omnis."
 *     },
 *     "relationships": {
 *       "blog": {
 *         "data": {
 *           "type": "blogs",
 *           "id": 30
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
@JsonApi(type = "entries", policy = Policy.SERIALIZATION_ONLY)
class EntryCreateRequest : Resource() {

    /**
     * Title of a new entry, plaintext.
     * Not always present
     */
    @field:Json(name = "title")
    var title: String? = null

    /**
     * Content of a new entry, in HTML format
     */
    @field:Json(name = "content")
    var content: String = ""

    /**
     * State of this entry. Variants: "draft", "published"
     */
    @field:Json(name = "state")
    lateinit var state: String

    /**
     * Blog that this entry belongs to.
     * Must be set if it's new entry.
     */
    var blog: HasOne<Blog>? = null
}

/**
 * Represents Entry in User -> Profile -> Blog -> Entry -> Comment relation
 *
 * Example:
 * ```
 * {
 *   "data": {
 *     "id": "16",
 *     "type": "entries",
 *     "links": {
 *       "self": "http://www.example.com/v1/entries/16"
 *     },
 *     "attributes": {
 *       "created-at": "2018-03-07T18:14:40.869Z",
 *       "updated-at": "2018-03-07T18:14:40.869Z",
 *       "content": "Debitis voluptas aut rem.",
 *       "state": "published",
 *       "title": "Aliquam sit repudiandae inventore."
 *     },
 *     "relationships": {
 *       "blog": {
 *         "links": {
 *           "self": "http://www.example.com/v1/entries/16/relationships/blog",
 *           "related": "http://www.example.com/v1/entries/16/blog"
 *         }
 *       },
 *       "comments": {
 *         "links": {
 *           "self": "http://www.example.com/v1/entries/16/relationships/comments",
 *           "related": "http://www.example.com/v1/entries/16/comments"
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
@JsonApi(type = "entries", policy = Policy.DESERIALIZATION_ONLY)
class EntryResponse: Resource() {

    /**
     * Date this entry was created, as returned by entries API request
     */
    @field:Json(name = "created-at")
    lateinit var createdAt: Date

    /**
     * Date this entry was updated, as returned by entries API request
     */
    @field:Json(name = "updated-at")
    lateinit var updatedAt: Date

    /**
     * Title of this entry, plaintext.
     * Not always present
     */
    @field:Json(name = "title")
    var title: String? = null

    /**
     * Content of this entry, in html format
     */
    @field:Json(name = "content")
    lateinit var content: String

    /**
     * State of this entry
     * @see EntryCreateRequest.state
     */
    @field:Json(name = "state")
    lateinit var state: String

    /**
     * Blog this entry belongs to
     */
    var blog = HasOne<Blog>()

    /**
     * Comments of this entry. This is link relationship, thus One-to-One
     */
    var comments = HasOne<Comment>()
}

typealias Entry = EntryResponse