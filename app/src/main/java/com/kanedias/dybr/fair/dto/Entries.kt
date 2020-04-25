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
 *       "profile": {
 *         "data": {
 *           "type": "profiles",
 *           "id": 30
 *         }
 *       },
 *       "community": {
 *         "data": {
 *           "type": "profiles",
 *           "id": 32
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
    @Json(name = "title")
    var title: String? = null

    /**
     * Content of a new entry, in HTML format
     */
    @Json(name = "content")
    var content: String = ""

    /**
     * Tags of this entry
     */
    @Json(name = "tags")
    var tags = listOf<String>()

    /**
     * State of this entry. Variants: "draft", "published"
     */
    @Json(name = "state")
    var state: String = "published"

    /**
     * Settings for publishing this entry. Include feed settings, privacy controls
     * and so on.
     *
     * If null, defaults are applied (visible for all and in feed)
     */
    @Json(name = "settings")
    var settings: RecordSettings? = null

    /**
     * Profile with blog this entry belongs to.
     * Must be set if it's new entry.
     */
    @Json(name = "profile")
    var profile: HasOne<OwnProfile>? = null

    /**
     * Profile with blog this entry is posted to.
     * Equals to [profile] if null.
     */
    @Json(name = "community")
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
 *       "profile": {
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
    @Json(name = "title")
    var title: String? = null

    /**
     * Content of this entry, in html format
     */
    @Json(name = "content")
    var content = ""

    /**
     * Tags of this entry
     */
    @Json(name = "tags")
    var tags = mutableListOf<String>()

    /**
     * State of this entry. May be "published" or "draft"
     * @see EntryCreateRequest.state
     */
    @Json(name = "state")
    var state = "published"

    /**
     * Reactions that are attached to this entry
     * @see Reaction
     */
    @Json(name = "reactions")
    var reactions: HasMany<Reaction>? = null

    /**
     * Settings for this entry. Include feed settings, privacy controls
     * and so on.
     *
     */
    @Json(name = "settings")
    var settings: RecordSettings? = null

    /**
     * Profile with blog this entry is posted to.
     * Equals to [profile] if null.
     */
    @Json(name = "community")
    var community: HasOne<OwnProfile>? = null

}

/**
 * Metadata of the retrieved entity. Contains info about properties not directly related to the entry
 * e.g. number of comments and commenting participants
 */
class EntryMeta {
    @Json(name = "commenters")
    var commenters = 0

    @Json(name = "comments")
    var comments = 0

    @Json(name = "pinned")
    var pinned: Boolean? = null

    @Json(name = "can-comment")
    var canComment: Boolean? = null

    @Json(name = "subscribed")
    var subscribed: Boolean? = null

    @Json(name = "bookmark")
    var bookmark: Boolean? = null
}

typealias Entry = EntryResponse

fun isEntryWritable(entry: Entry?): Boolean {
    if (entry == null)
        return false

    if (Auth.profile == null)
        return false

    val meta = Network.bufferToObject(entry.meta, EntryMeta::class.java)
    if (meta?.canComment == false)
        return false

    return true
}