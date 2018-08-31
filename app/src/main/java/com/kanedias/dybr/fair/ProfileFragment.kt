package com.kanedias.dybr.fair

import android.app.Dialog
import android.app.FragmentTransaction
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.Blog
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.misc.idMatches
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Kanedias
 *
 * Created on 20.07.18
 */
class ProfileFragment: DialogFragment() {

    @BindView(R.id.author_avatar)
    lateinit var authorAvatar: ImageView

    @BindView(R.id.author_name)
    lateinit var authorName: TextView

    @BindView(R.id.author_registration_date)
    lateinit var registrationDate: TextView

    @BindView(R.id.author_blog)
    lateinit var authorBlog: TextView

    @BindView(R.id.author_add_to_favorites)
    lateinit var favoritesToggle: ImageView

    lateinit var profile: OwnProfile

    private lateinit var filledStar: Drawable
    private lateinit var emptyStar: Drawable

    private lateinit var activity: MainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.getSerializable("profile")?.let { profile = it as OwnProfile }

        activity = context as MainActivity

        val view = activity.layoutInflater.inflate(R.layout.fragment_profile, null)
        ButterKnife.bind(this, view)
        setupUI()

        return MaterialDialog.Builder(activity)
                .title(R.string.view_profile)
                .customView(view, true)
                .neutralText(android.R.string.ok)
                .build()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("profile", profile)
    }

    @Suppress("DEPRECATION") // we need to support API < 24 in Html.fromHtml and setImageDrawable
    private fun setupUI() {
        filledStar = activity.resources.getDrawable(R.drawable.star_filled)
        emptyStar = activity.resources.getDrawable(R.drawable.star_border)

        // set names and dates
        authorName.text = profile.nickname
        registrationDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(profile.createdAt)

        val blog = profile.blogs.get(profile.document)?.first()
        if (blog != null) {
            authorBlog.text = Html.fromHtml("<a href='https://dybr.ru/blog/${blog.slug}'>${blog.title}</a>")
            authorBlog.setOnClickListener { dismiss(); showBlog(blog) }
        } else {
            authorBlog.text = ""
        }

        // set avatar
        val avatarUrl = profile.settings?.avatar
        if (!avatarUrl.isNullOrBlank()) {
            // load avatar asynchronously
            launch(UI) {
                try {
                    val parsed = URI.create(avatarUrl)
                    val destination = when(parsed.isAbsolute) {
                        true -> HttpUrl.get(parsed)
                        false -> HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT)?.resolve(avatarUrl!!)
                    } ?: return@launch
                    val req = Request.Builder().url(destination).build()
                    val bitmap = async {
                        val resp = Network.httpClient.newCall(req).execute()
                        BitmapFactory.decodeStream(resp.body()?.byteStream())
                    }.await()
                    authorAvatar.setImageBitmap(bitmap)
                } catch (ioex: IOException) {
                    Network.reportErrors(activity, ioex)
                }
            }
        } else {
            authorAvatar.setImageDrawable(ColorDrawable(activity.resources.getColor(R.color.md_grey_600)))
        }

        // set favorite status
        when {
            Auth.profile?.favorites?.any { it.idMatches(profile) } == true -> favoritesToggle.setImageDrawable(filledStar)
            else -> favoritesToggle.setImageDrawable(emptyStar)
        }
    }

    @OnClick(R.id.author_add_to_favorites)
    fun toggleFavorite() {
        // if it's clicked then it's visible
        launch(UI) {
            try {
                if (Auth.profile?.favorites?.any { it.idMatches(profile) } == true) {
                    // remove from favorites
                    async { Network.removeFavorite(profile) }.await()
                    Auth.profile?.favorites?.remove(profile)
                    favoritesToggle.setImageDrawable(emptyStar)
                    Toast.makeText(activity, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show()
                } else {
                    // add to favorites
                    async { Network.addFavorite(profile) }.await()
                    Auth.profile?.favorites?.add(profile)
                    favoritesToggle.setImageDrawable(filledStar)
                    Toast.makeText(activity, R.string.added_to_favorites, Toast.LENGTH_SHORT).show()
                }
            } catch (ioex: IOException) {
                Network.reportErrors(activity, ioex)
            }
        }
    }

    private fun showBlog(blog: Blog) {
        val browseFragment = EntryListFragmentFull().apply { this.blog = blog }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing blog from profile")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, browseFragment)
                .commit()
    }
}