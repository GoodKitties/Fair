package com.kanedias.dybr.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.*
import java.util.*

/**
 * Favorite creation request.
 * Example:
 * ```
 * {
 *     "data": {
 *         "id": "1",
 *         "relationships": {
 *             "subscription": {
 *                 "data": {
 *                     "id": "140",
 *                     "type": "profiles"
 *                 }
 *             },
 *             "reader": {
 *                 "data": {
 *                     "id": "32",
 *                     "type": "profiles"
 *                 }
 *             }
 *         },
 *         "type": "favorites"
 *     }
 * }
 * ```
 * @author Kanedias
 *
 * Created on 19.07.18
 */
@JsonApi(type = "favorites", policy = Policy.SERIALIZATION_ONLY)
class CreateFavoriteRequest: Resource() {

    /**
     * Link to favorite blog to persist
     */
    @field:Json(name = "subscription")
    var subscription: HasOne<OwnProfile>? = null

    /**
     * Our own profile, which will be the reader of the favorite blog
     */
    @field:Json(name = "reader")
    var reader: HasOne<OwnProfile>? = null
}

/**
 * Represents comment in User -> Profile -> Blog -> Entry -> Comment hierarchy.
 * Example:
 * ```
 * {
 *     "data": {
 *         "id": "1",
 *         "relationships": {
 *             "profiles": {
 *                 "data": {
 *                     "id": "32",
 *                     "type": "profiles"
 *                 }
 *             },
 *             "subscriptions": {
 *                 "data": [
 *                     {
 *                         "id": "125",
 *                         "type": "profiles"
 *                     }
 *                 ]
 *             }
 *         },
 *         "type": "favorites"
 *     }
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 18.07.18
 */
@JsonApi(type = "favorites", policy = Policy.DESERIALIZATION_ONLY)
class FavoritesResponse: Resource() {

    /**
     * Link to favorite blog to persist
     */
    @field:Json(name = "profiles")
    var profile: HasOne<OwnProfile> = HasOne()

    /**
     * Our own profile, which will be the reader of the favorite blog
     */
    @field:Json(name = "subscriptions")
    var subscriptions: HasMany<OwnProfile> = HasMany()
}

typealias Favorites = FavoritesResponse