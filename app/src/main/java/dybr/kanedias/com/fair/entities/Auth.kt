package dybr.kanedias.com.fair.entities

import android.content.Context
import dybr.kanedias.com.fair.R

/**
 * Auth static entity providing info about user login status
 *
 * @author Kanedias
 *
 * Created on 05.11.17
 */
object Auth {
    lateinit var guest: Account
    lateinit var user: Account

    fun init(ctx: Context) {
        guest = Account().apply { email = ctx.getString(R.string.guest) }
        user = guest
    }
}