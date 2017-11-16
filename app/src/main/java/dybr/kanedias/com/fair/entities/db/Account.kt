package dybr.kanedias.com.fair.entities.db

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable

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
class Account {
    /**
     * Inner id in the database, not used
     */
    @DatabaseField(generatedId = true)
    var id: Long = 0

    /**
     * E-Mail user when registered
     */
    @DatabaseField(canBeNull = false, unique = true)
    var email: String = ""

    /**
     * Chosen nickname
     */
    @DatabaseField(canBeNull = false, unique = true)
    var name: String = ""

    /**
     * Password in plaintext. In Android all application data
     * is buried under {@code rwx------} folder visible only to it,
     * so we don't have to worry about it being non-encoded.
     */
    @DatabaseField(canBeNull = false)
    var password: String = ""

    /**
     * Whether this is current account for the app
     */
    @DatabaseField(canBeNull = false)
    var current: Boolean = false

    // current identity for this user
    lateinit var identity: Identity
}