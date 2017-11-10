package dybr.kanedias.com.fair.entities.db

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable

/**
 * Basic account class with persistence layer.
 *
 * @author Kanedias
 *
 * Created on 11.11.17
 */
@DatabaseTable(tableName = "account")
class Account {

    @DatabaseField(id = true, generatedId = true)
    var id: Long = 0

    /**
     * E-Mail (used as username) as in login field on dybr.ru
     */
    @DatabaseField(canBeNull = false)
    var email: String = ""

    /**
     * Chosen username
     */
    @DatabaseField(canBeNull = false)
    var name: String = ""

    // don't grab identities, don't need them yet

    /**
     * Password in plaintext. In Android all application data
     * is buried under {@code rwx------} folder visible only to it,
     * so we don't have to worry about it being non-encoded.
     */
    @DatabaseField(canBeNull = false)
    var password: String = ""
}