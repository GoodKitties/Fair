package dybr.kanedias.com.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Identity is essentially a profile with readers/favorites, diary info, address and so on.
 * Example (result of `current_identity` call):
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
class Identity: Resource() {

    /**
     * Server-side identity id
     */
    @Json(name = "_id")
    var identityId: String = ""

    // not interested in updated/created timestamps yet
    // and it seems that they are subject to change, e.g. "changedAt" --> "changed" etc.
    //var changedAt: Date? = null
    //var createdAt: Date? = null

    /**
     * Chosen nickname
     */
    var name: String = ""

    /**
     * Main diary
     */
    var diary: Diary? = null

    /**
     * Readers (subscribers) of this profile
     */
    val readers: MutableList<Identity> = ArrayList()

    /**
     * Favorites (subscribed to) of this profile
     */
    val favorites: MutableList<Identity> = ArrayList()

    /**
     * Array of all assigned URIs (diary address paths) for this user.
     */
    @Json(name = "uri")
    var uris: MutableList<String> = ArrayList()
}