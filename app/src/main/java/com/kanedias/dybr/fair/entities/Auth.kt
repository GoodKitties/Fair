package com.kanedias.dybr.fair.entities

import android.content.Context
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.database.DbProvider

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

    /**
     * Current profile after it's loaded
     * Filled in [Auth.updateCurrentProfile]
     */
    var profile: OwnProfile? = null

    /**
     * Blog of currently loaded [profile]
     */
    var blog: Blog? = null

    fun init(ctx: Context) {
        this.guest = Account().apply { email = ctx.getString(R.string.guest) }
        this.user = guest
    }

    fun updateCurrentUser(acc: Account) {
        this.user = acc
        this.profile = null
        this.blog = null
    }

    fun updateCurrentProfile(prof: OwnProfile) {
        this.user.lastProfileId = prof.id
        this.profile = prof
        this.blog = null

        DbProvider.helper.accDao.update(Auth.user)
    }

    fun updateBlog(blog: Blog) {
        this.blog = blog
    }
}