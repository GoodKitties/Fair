package dybr.kanedias.com.fair.entities

import com.google.gson.annotations.SerializedName
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

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
@DatabaseTable(tableName = "identity")
class Identity {

    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    /**
     * Server-side identity id
     */
    @SerializedName("_id")
    @DatabaseField(index = true, canBeNull = false)
    var identityId: String = ""

    // not interested in updated/created timestamps

    /**
     * Chosen nickname
     */
    @DatabaseField(canBeNull = false)
    var name: String = ""

    /**
     * Main diary
     */
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    var diary: Diary? = null

    /**
     * Readers (subscribers) of this profile
     */
    val readers: MutableList<@JvmSuppressWildcards Identity> = ArrayList()

    /**
     * Favorites (subscribed to) of this profile
     */
    val favorites: MutableList<Identity> = ArrayList()

    /**
     * Array of all assigned URIs (diary address paths) for this user.
     */
    @SerializedName("uri")
    var uris: MutableList<String> = ArrayList()
}