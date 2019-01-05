package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.*
import java.io.Serializable

/**
 * Profile creation request.
 * Example of request body:
 * ```
 * {
 *   "data": {
 *     "type": "profiles",
 *     "attributes": {
 *       "nickname": "olaf",
 *       "birthday": "02-01",
 *       "description": "Repellendus tempore vel qui quia. "
 *     }
 *   }
 * }
 * ```
 */
@JsonApi(type = "profiles", policy = Policy.SERIALIZATION_ONLY)
class ProfileCreateRequest: Resource() {

    /**
     * Chosen nickname
     */
    @field:Json(name = "nickname")
    lateinit var nickname: String

    /**
     * Birthday in DD-MM format
     */
    @field:Json(name = "birthday")
    lateinit var birthday: String

    /**
     * Description of this profile (multiline text)
     */
    @field:Json(name = "description")
    lateinit var description: String
}

/**
 * OwnProfile is an actual identity that user wants to be associated with.
 * Has description, nickname, readers/world, diary info, address and so on.
 * Example (result of `/v2/profiles/{id}` call):
 * ```
 * {
 *   "data": {
 *     "id": "2",
 *     "type": "profiles",
 *     "links": {
 *       "self": "http://www.example.com/v2/profiles/2"
 *     },
 *     "attributes": {
 *       "created-at": "2017-12-17T18:36:31.555Z",
 *       "updated-at": "2017-12-17T18:36:31.555Z",
 *       "nickname": "lourdes_nader",
 *       "description": "Modi reiciendis accusantium. Mollitia dolores iste. Exercitationem autem velit soluta et. Dolorem voluptas omnis.",
 *       "birthday": "14-06",
 *       "settings": {
 *         "key": "value"
 *       }
 *     },
 *     "relationships": {
 *       "user": {
 *         "links": {
 *           "self": "http://www.example.com/v2/profiles/2/relationships/user",
 *           "related": "http://www.example.com/v2/profiles/2/user"
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 * @author Kanedias
 *
 * Created on 17.11.17
 */
@JsonApi(type = "profiles", policy = Policy.DESERIALIZATION_ONLY)
class ProfileResponse : Dated() {

    /**
     * Chosen nickname
     */
    @field:Json(name = "nickname")
    lateinit var nickname: String

    /**
     * Birthday in DD-MM format
     */
    @field:Json(name = "birthday")
    lateinit var birthday: String

    /**
     * Slug of the blog associated with this profile
     */
    @field:Json(name = "blog-slug")
    var blogSlug: String? = null

    /**
     * Title of the blog associated with this profile
     */
    @field:Json(name = "blog-title")
    var blogTitle: String? = null

    /**
     * Preferences structure for this profile
     */
    @field:Json(name = "settings")
    var settings: ProfileSettings? = null

    /**
     * Tags that this profile is using
     */
    @field:Json(name = "tags")
    var tags = mutableSetOf<Tag>()

    /**
     * Link to the user this profile belongs to
     */
    @field:Json(name = "user")
    var user = HasOne<User>()

    /**
     * Link to the readers of this profile
     */
    @field:Json(name = "readers")
    var readers = HasMany<OwnProfile>()

    /**
     * Link to the subscribers of this profile
     */
    @field:Json(name = "favorites")
    var favorites = HasMany<OwnProfile>()
}

data class Tag(
        val name: String,
        var entries: Int
) : Serializable

typealias OwnProfile = ProfileResponse