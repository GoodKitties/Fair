package dybr.kanedias.com.fair.entities

import com.j256.ormlite.field.DataType
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Resource
import java.util.*

/**
 * Account class for logging in.
 *
 * A distinction between this and [Identity] is that account
 * holds only auth and basic naming info which is stored after registration or first login.
 * The account itself is almost always immutable, whereas [Identity] is subject to change
 * on favourite/subscribers additions.
 *
 * @author Kanedias
 *
 * Created on 14.11.17
 */
@DatabaseTable(tableName = "account")
@JsonApi(type = "users")
class Account: Resource() {
    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    /**
     * E-Mail user when registered
     */
    @DatabaseField(canBeNull = false, unique = true)
    lateinit var email: String

    /**
     * Password in plaintext. In Android all application data
     * is buried under {@code rwx------} folder visible only to it,
     * so we don't have to worry about it being non-encoded.
     */
    @DatabaseField(canBeNull = false)
    lateinit var password: String

    @DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false)
    lateinit var createdAt: Date

    @DatabaseField(dataType = DataType.DATE_LONG, canBeNull = false)
    lateinit var updatedAt: Date

    @DatabaseField(canBeNull = false)
    var isOver18: Boolean = false

    /**
     * Whether this is current account for the app
     */
    @DatabaseField(canBeNull = false)
    var current: Boolean = false
}