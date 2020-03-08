package com.kanedias.dybr.fair.dto

import com.kanedias.dybr.fair.Network
import com.squareup.moshi.Json
import moe.banana.jsonapi2.*

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
     * Tags of this entry
     */
    @field:Json(name = "tags")
    var tags = listOf<String>()

    /**
     * State of this entry. Variants: "draft", "published"
     */
    @field:Json(name = "state")
    var state: String = "published"

    /**
     * Settings for publishing this entry. Include feed settings, privacy controls
     * and so on.
     *
     * If null, defaults are applied (visible for all and in feed)
     */
    @field:Json(name = "settings")
    var settings: RecordSettings? = null

    /**
     * Profile with blog this entry belongs to.
     * Must be set if it's new entry.
     */
    @field:Json(name = "profile")
    var profile: HasOne<OwnProfile>? = null

    /**
     * Profile with blog this entry is posted to.
     * Equals to [profile] if null.
     */
    @field:Json(name = "community")
    var community: HasOne<OwnProfile>? = null
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
 *       "reactions" : {
 *         "data": [ ... ]
 *       }
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
class EntryResponse: Authored() {

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
    var content = ""

    /**
     * Tags of this entry
     */
    @field:Json(name = "tags")
    var tags = mutableListOf<String>()

    /**
     * State of this entry. May be "published" or "draft"
     * @see EntryCreateRequest.state
     */
    @field:Json(name = "state")
    var state = "published"

    /**
     * Reactions that are attached to this entry
     * @see Reaction
     */
    @field:Json(name = "reactions")
    var reactions: HasMany<Reaction>? = null

    /**
     * Settings for this entry. Include feed settings, privacy controls
     * and so on.
     *
     */
    @field:Json(name = "settings")
    var settings: RecordSettings? = null

    /**
     * Profile with blog this entry is posted to.
     * Equals to [profile] if null.
     */
    @field:Json(name = "community")
    var community: HasOne<OwnProfile>? = null

}

/**
 * Metadata of the retrieved entity. Contains info about properties not directly related to the entry
 * e.g. number of comments and commenting participants
 */
class EntryMeta {
    @field:Json(name = "commenters")
    var commenters = 0

    @field:Json(name = "comments")
    var comments = 0

    @field:Json(name = "pinned")
    var pinned: Boolean? = null

    @field:Json(name = "can-comment")
    var canComment: Boolean? = null

    @field:Json(name = "subscribed")
    var subscribed: Boolean? = null

    @field:Json(name = "bookmark")
    var bookmark: Boolean? = null
}

typealias Entry = EntryResponse

fun isEntryWritable(entry: Entry?): Boolean {
    if (entry == null)
        return false

    if (Auth.profile == null)
        return false

    val meta = Network.bufferToObject<EntryMeta>(entry.meta)
    if (meta?.canComment == false)
        return false

    return true
}