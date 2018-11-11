package com.kanedias.dybr.fair.database.entities

import android.widget.TextView
import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

/**
 * An entity representing an offline draft.
 * A draft contains just some text content and the date it was created, for easy sorting.
 *
 * @author Kanedias
 *
 * Created on 22.08.18
 */
@DatabaseTable(tableName = "draft")
class OfflineDraft {

    constructor()

    constructor(key: String? = null, title: TextView? = null, base: TextView) {
        this.key = key
        this.title = title?.text?.toString()
        this.content = base.text.toString()
    }

    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    /**
     * Date this draft was created
     */
    @DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false)
    var createdAt = Date()

    /**
     * Key to find this draft easily, must be unique
     */
    @DatabaseField(index = true, canBeNull = true)
    var key: String? = null

    /**
     * Title of this draft, if applicable
     */
    @DatabaseField(canBeNull = true)
    var title: String? = null

    /**
     * Contents of this draft
     */
    @DatabaseField(canBeNull = false)
    lateinit var content: String
}