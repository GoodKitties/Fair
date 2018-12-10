package com.kanedias.dybr.fair.dto

import android.content.Context
import com.kanedias.dybr.fair.R
import com.squareup.moshi.Json
import java.io.Serializable

/**
 * Generic settings structure for a record ([Entry] or [Comment])
 *
 * (Don't confuse this class with [ProfileSettings] which are linked to [OwnProfile] instead)
 */
data class RecordSettings (
    @field:Json(name = "permissions")
    val permissions: RecordPermissions
) : Serializable

/**
 * Entity representing record (entry or comment) permissions.
 * Part of the [RecordSettings] entity.
 *
 * if access list item is present but null, this access item is deleted. Use it to revert
 * entry permissions to "same as blog"
 */
data class RecordPermissions (
        /**
         * Record access items, combined they represent complete permission scheme for
         * a record (entry or comment).
         *
         * The items are applied from first to last for more complex scenarios.
         */
        @field:Json(name = "access")
        val access: List<RecordAccessItem?>
) : Serializable

/**
 * Record access item, representing part of the whole permission scheme for entry or comment.
 * Part of [RecordPermissions].
 *
 * This allows setting permissions for an entry like this:
 * - Only for the owner
 * - Only for registered users
 * - Only for favorites of a current profile
 *
 * Corresponding type/allow combinations (for now) are:
 *      "private/false", "registered/true", "favorites/true"
 */
data class RecordAccessItem (
        /**
         * type of access item. Can be "private", "registered", "favorites"
         */
        @field:Json(name = "type")
        val type: String,

        /**
         * Allow or deny this type of access
         */
        @field:Json(name = "allow")
        val allow: Boolean
) : Serializable

infix fun RecordAccessItem?.toDescription(ctx: Context) : String {
    val descArray = ctx.resources.getStringArray(R.array.permission_types)
    return when (this) {
        RecordAccessItem("private", false) -> descArray[0]
        RecordAccessItem("registered", true) -> descArray[1]
        RecordAccessItem("favorites", true) -> descArray[2]
        else -> descArray[3] // visible for all
    }
}