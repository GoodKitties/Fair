package com.kanedias.dybr.fair.dto

import android.content.Context
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.Account

/**
 * Auth static entity providing info about user login status
 *
 * @author Kanedias
 *
 * Created on 05.11.17
 */
object Auth {
    // service variables, changed only once
    lateinit var guest: Account
    lateinit var emptyBlogMarker: Blog
    lateinit var favoritesMarker: Blog
    lateinit var worldMarker: Blog

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
        // setup special values
        this.emptyBlogMarker = Blog().apply { id = "dummy" }
        this.worldMarker = Blog().apply { id = "world"; title = ctx.getString(R.string.world) }
        this.favoritesMarker = Blog().apply { id = "favorites"; title = ctx.getString(R.string.favorite) }
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