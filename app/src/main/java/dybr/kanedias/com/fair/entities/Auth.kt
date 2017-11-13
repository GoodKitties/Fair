package dybr.kanedias.com.fair.entities

import dybr.kanedias.com.fair.entities.db.Identity

/**
 * Auth static entity providing info about user login status
 *
 * @author Kanedias
 *
 * Created on 05.11.17
 */
object Auth {
    var user: Identity? = null
}