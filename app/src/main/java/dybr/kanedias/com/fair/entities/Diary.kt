package dybr.kanedias.com.fair.entities

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import com.squareup.moshi.Json

/**
 * @author Kanedias
 *
 * Created on 14.11.17
 */
@DatabaseTable(tableName = "diary")
class Diary {

    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    /**
     * server-side ID of this diary
     */
    @Json(name = "_id")
    @DatabaseField(index = true, canBeNull = false)
    var diaryId: String = ""

    /**
     * Title of the diary, this is how this diary is named
     * when it's created on registration
     */
    @DatabaseField(canBeNull = false)
    var title: String = ""

    /**
     * Epigraph of the diary
     */
    @DatabaseField(canBeNull = true)
    var epigraph: String? = null

    /**
     * image (avatar link?) of this diary
     */
    @DatabaseField(canBeNull = true)
    var image: String? = null

}