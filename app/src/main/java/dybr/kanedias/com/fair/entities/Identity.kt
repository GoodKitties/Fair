package dybr.kanedias.com.fair.entities

import com.google.gson.annotations.SerializedName
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
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
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    var diary: Diary? = Diary()

    /**
     * Array of all assigned URIs (diary address paths) for this user.
     */
    @SerializedName("uri")
    var uris: ArrayList<String> = ArrayList()
}