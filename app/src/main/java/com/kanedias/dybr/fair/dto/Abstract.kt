package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasOne
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Entity that has creation and modification date
 *
 * @author Kanedias
 *
 * Created on 04.01.19
 */
open class Dated: Resource() {

    /**
     * Date this entity was created at.
     * Immutable
     */
    @field:Json(name = "created-at")
    lateinit var createdAt: Date

    /**
     * Date this entity was last modified at
     */
    @field:Json(name = "updated-at")
    lateinit var updatedAt: Date

}

/**
 * Entity that is authored by someone
 *
 * @author Kanedias
 *
 * Created on 04.01.19
 */
open class Authored: Dated() {

    /**
     * Profile this entity was created by
     */
    @field:Json(name = "profile")
    val profile = HasOne<OwnProfile>()
}