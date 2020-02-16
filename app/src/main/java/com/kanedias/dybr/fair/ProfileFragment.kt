package com.kanedias.dybr.fair

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.text.Html
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.StyleLevel
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.misc.idMatches
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import okhttp3.Request
import java.io.IOException
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

    @BindViews(
            R.id.author_name_label,
            R.id.author_registration_date_label,
            R.id.author_blog_label
    )
    lateinit var labels: List<@JvmSuppressWildcards TextView>

    lateinit var profile: OwnProfile

    private lateinit var filledStar: Drawable
    private lateinit var emptyStar: Drawable

    private lateinit var activity: MainActivity

    private lateinit var styleLevel: StyleLevel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.getSerializable("profile")?.let { profile = it as OwnProfile }

        activity = context as MainActivity

        val view = activity.layoutInflater.inflate(R.layout.fragment_profile, null)
        ButterKnife.bind(this, view)
        setupUI()

        return MaterialDialog(activity)
                .title(R.string.view_profile)
                .customView(view = view, scrollable = true)
                .positiveButton(android.R.string.ok)
    }

    override fun onResume() {
        super.onResume()
        setupTheming(dialog as MaterialDialog)
    }

    private fun setupTheming(dialog: MaterialDialog) {
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        val dialogTitle = dialog.view.titleLayout.findViewById(R.id.md_text_title) as TextView
        val okButton = dialog.view.buttonsLayout!!.findViewById(R.id.md_button_positive) as DialogActionButton

        styleLevel.bind(TEXT_BLOCK, dialog.view, BackgroundNoAlphaAdapter())
        styleLevel.bind(TEXT_HEADERS, dialogTitle, TextViewColorAdapter())
        styleLevel.bind(TEXT_LINKS, okButton, MaterialDialogButtonAdapter())

        styleLevel.bind(TEXT, authorName)
        styleLevel.bind(TEXT, registrationDate)
        styleLevel.bind(TEXT, authorBlog)
        styleLevel.bind(TEXT_LINKS, authorBlog, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, favoritesToggle)

        labels.forEach { styleLevel.bind(TEXT, it) }
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

        if (profile.blogSlug != null) {
            authorBlog.text = Html.fromHtml("<a href='https://dybr.ru/blog/${profile.blogSlug}'>${profile.blogTitle}</a>")
            authorBlog.setOnClickListener { dismiss(); showBlog(profile) }
        } else {
            authorBlog.text = ""
        }

        // set avatar
        val avatarUrl = profile.settings.avatar
        if (!avatarUrl.isNullOrBlank()) {
            // resolve URL if it's not absolute
            val resolved = Network.resolve(avatarUrl) ?: return

            // load avatar asynchronously
            Glide.with(authorAvatar)
                    .load(resolved.toString())
                    .apply(RequestOptions().centerInside())
                    .into(authorAvatar)
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
        lifecycleScope.launch {
            try {
                if (Auth.profile?.favorites?.any { it.idMatches(profile) } == true) {
                    // remove from favorites
                    withContext(Dispatchers.IO) { Network.removeFavorite(profile) }
                    Auth.profile?.favorites?.remove(profile)
                    favoritesToggle.setImageDrawable(emptyStar)
                    Toast.makeText(activity, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show()
                } else {
                    // add to favorites
                    withContext(Dispatchers.IO) { Network.addFavorite(profile) }
                    Auth.profile?.apply {
                        favorites.add(profile)
                        document.addInclude(profile)
                    }
                    favoritesToggle.setImageDrawable(filledStar)
                    Toast.makeText(activity, R.string.added_to_favorites, Toast.LENGTH_SHORT).show()
                }
            } catch (ioex: IOException) {
                Network.reportErrors(context, ioex)
            }
        }
    }

    private fun showBlog(profile: OwnProfile) {
        val browseFragment = EntryListFragmentFull().apply { this.profile = profile }
        activity.showFullscreenFragment(browseFragment)
    }
}