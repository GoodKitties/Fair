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
    var accountId: String = ""

    // not interested in updated/created timestamps

    /**
     * Chosen nickname
     */
    @DatabaseField(canBeNull = false)
    var name: String = ""

    /**
     * Main diary address (dybr.ru/<address>)
     */
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoCreate = true, foreignAutoRefresh = true)
    val diary: Diary = Diary()

    @SerializedName("uri")
    @DatabaseField(dataType = DataType.SERIALIZABLE)
    val uris: ArrayList<String> = ArrayList()
}