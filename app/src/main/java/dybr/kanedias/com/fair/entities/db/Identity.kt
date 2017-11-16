package dybr.kanedias.com.fair.entities.db

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable

/**
 * Basic identity class with persistence layer.
 *
 * @author Kanedias
 *
 * Created on 11.11.17
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
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    val diary: Diary = Diary()

    /**
     * Array of all assigned URIs (diary address paths) for this user.
     */
    @SerializedName("uri")
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    val uris: ArrayList<String> = ArrayList()

    // non-db fields, retrieved on deserialization of API queries
    val readers: List<PartialIdentity> = ArrayList()
    val favorites: List<PartialIdentity> = ArrayList()
}

/**
 * Identity that doesn't contain a diary.
 * These are usually found in `readers` or `favourites` of main identity
 */
data class PartialIdentity(
        @SerializedName("_id")
        val identityId: String,

        @SerializedName("name")
        val name: String,

        @SerializedName("uri")
        val uris: List<String> = ArrayList()
)