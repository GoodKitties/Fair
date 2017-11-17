package dybr.kanedias.com.fair.entities

import com.google.gson.annotations.SerializedName

/**
 * Profile is essentially an identity with readers/favorites.
 * Example:
 * ```
 * {
 *   "identity": {
 *     "_id": "5a088c8888c010001d9fb9f6",
 *     "name": "Example",
 *     "diary": {
 *       "title": "Example title",
 *       "_id": "5a088c8888c010001d9fb9f7"
 *     },
 *     "readers": [...],
 *     "favorites": [...]
 *   }
 * }
 * ```
 * @author Kanedias
 *
 * Created on 17.11.17
 */
class Profile {
    /**
     * Identity of this profile, provides name and diary location
     */
    lateinit var identity: Identity

    /**
     * Readers (subscribers) of this profile
     */
    @JvmField
    var readers: List<Identity> = ArrayList()

    /**
     * Favorites (subscribed to) of this profile
     */
    var favorites: List<Identity> = ArrayList()

    /**
     * Array of all assigned URIs (diary address paths) for this user.
     */
    @Deprecated("Will be part of Identity soon")
    @SerializedName("uri")
    var uris: ArrayList<String> = ArrayList()
}