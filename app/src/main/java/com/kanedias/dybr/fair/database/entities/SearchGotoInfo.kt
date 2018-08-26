package com.kanedias.dybr.fair.database.entities

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

/**
 * @author Kanedias
 *
 * Created on 25.08.18
 */
@DatabaseTable(tableName = "search_goto")
class SearchGotoInfo {

    constructor()

    constructor(type: EntityType, name: String) {
        this.type = type
        this.name = name
    }

    enum class EntityType { PROFILE, BLOG }

    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    @DatabaseField(canBeNull = false, uniqueCombo = true, dataType = DataType.ENUM_STRING)
    lateinit var type: EntityType

    @DatabaseField(canBeNull = false, uniqueCombo = true, index = true)
    lateinit var name: String
}