package com.kanedias.dybr.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.*
import java.io.Serializable
import java.util.*

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
 * Example (result of `/v1/own-profiles/{id}` call):
 * ```
 * {
 *   "data": {
 *     "id": "2",
 *     "type": "profiles",
 *     "links": {
 *       "self": "http://www.example.com/v1/own-profiles/2"
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
 *           "self": "http://www.example.com/v1/own-profiles/2/relationships/user",
 *           "related": "http://www.example.com/v1/own-profiles/2/user"
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
class ProfileResponse : Resource() {

    /**
     * Chosen nickname
     */
    @field:Json(name = "nickname")
    lateinit var nickname: String

    /**
     * Date this profile was created, as returned by users API request
     */
    @field:Json(name = "created-at")
    lateinit var createdAt: Date

    /**
     * Date this profile was updated, as returned by users API request
     */
    @field:Json(name = "updated-at")
    lateinit var updatedAt: Date

    /**
     * Birthday in DD-MM format
     */
    @field:Json(name = "birthday")
    lateinit var birthday: String

    /**
     * Settings structure for this profile
     */
    @field:Json(name = "settings")
    var settings: ProfileSettings? = null

    /**
     * Link to the user this profile belongs to
     */
    @field:Json(name = "user")
    var user = HasOne<User>()

    /**
     * Link to the blog of this profile
     */
    @field:Json(name = "blogs")// TODO: fix on backend
    var blogs = HasMany<Blog>()

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

typealias OwnProfile = ProfileResponse