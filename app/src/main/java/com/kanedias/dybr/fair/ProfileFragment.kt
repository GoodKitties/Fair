package com.kanedias.dybr.fair

import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.text.Html
import android.view.View
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
import com.kanedias.dybr.fair.dto.ActionList
import com.kanedias.dybr.fair.dto.ActionListRequest
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.misc.idMatches
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import kotlinx.coroutines.*
import moe.banana.jsonapi2.HasMany
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

    @BindView(R.id.author_subtext)
    lateinit var authorSubtext: TextView

    @BindView(R.id.author_registration_date)
    lateinit var registrationDate: TextView

    @BindView(R.id.author_blog)
    lateinit var authorBlog: TextView

    @BindView(R.id.author_add_to_favorites)
    lateinit var favoritesToggle: ImageView

    @BindView(R.id.author_feed_ban)
    lateinit var feedBanToggle: ImageView

    @BindView(R.id.author_ban)
    lateinit var banToggle: ImageView

    @BindViews(
            R.id.author_name_label,
            R.id.author_subtext_label,
            R.id.author_registration_date_label,
            R.id.author_blog_label
    )
    lateinit var labels: List<@JvmSuppressWildcards TextView>

    lateinit var profile: OwnProfile
    var actionLists: MutableList<ActionList> = mutableListOf()

    private lateinit var accountFavorited: Drawable
    private lateinit var accountUnfavorited: Drawable

    private lateinit var accountFeedBanned: Drawable
    private lateinit var accountFeedUnbanned: Drawable

    private lateinit var accountBanned: Drawable
    private lateinit var accountUnbanned: Drawable

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
        styleLevel.bind(TEXT, authorSubtext)
        styleLevel.bind(TEXT, registrationDate)
        styleLevel.bind(TEXT, authorBlog)
        styleLevel.bind(TEXT_LINKS, authorBlog, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, favoritesToggle)
        styleLevel.bind(TEXT_LINKS, feedBanToggle)
        styleLevel.bind(TEXT_LINKS, banToggle)

        labels.forEach { styleLevel.bind(TEXT, it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("profile", profile)
    }

    @Suppress("DEPRECATION") // we need to support API < 24 in Html.fromHtml and setImageDrawable
    private fun setupUI() {
        accountFavorited = activity.resources.getDrawable(R.drawable.account_favorited)
        accountUnfavorited = activity.resources.getDrawable(R.drawable.account_unfavorited)
        accountFeedBanned = activity.resources.getDrawable(R.drawable.account_feed_banned)
        accountFeedUnbanned = activity.resources.getDrawable(R.drawable.account_feed_unbanned)
        accountBanned = activity.resources.getDrawable(R.drawable.account_banned)
        accountUnbanned = activity.resources.getDrawable(R.drawable.account_unbanned)

        // set names and dates
        authorName.text = profile.nickname
        authorSubtext.text = profile.settings.subtext
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
            Auth.profile?.favorites?.any { it.idMatches(profile) } == true -> favoritesToggle.setImageDrawable(accountFavorited)
            else -> favoritesToggle.setImageDrawable(accountUnfavorited)
        }

        // set feed banned status
        lifecycleScope.launch {
            Network.perform(
                networkAction = { Network.loadActionLists() },
                uiAction = { loaded ->
                    actionLists = loaded
                    setupActionLists()
                }
            )
        }
    }

    private fun setupActionLists() {
        val feedBanned = actionLists.filter { it.action == "hide" && it.scope == "feed" }
                .flatMap { list -> list.profiles.get() }
                .any { prof -> prof.idMatches(profile) }

        feedBanToggle.visibility = View.VISIBLE
        when {
            feedBanned -> feedBanToggle.setImageDrawable(accountFeedBanned)
            else -> feedBanToggle.setImageDrawable(accountFeedUnbanned)
        }

        val accBanned = actionLists.filter { it.action == "ban" && it.scope == "blog" }
                .flatMap { list -> list.profiles.get() }
                .any { prof -> prof.idMatches(profile) }

        banToggle.visibility = View.VISIBLE
        when {
            accBanned -> banToggle.setImageDrawable(accountBanned)
            else -> banToggle.setImageDrawable(accountUnbanned)
        }
    }

    @OnClick(R.id.author_feed_ban)
    fun toggleFeedBan() {
        val feedBanned = actionLists.filter { it.action == "hide" && it.scope == "feed" }
                                    .flatMap { list -> list.profiles.get() }
                                    .any { prof -> prof.idMatches(profile) }

        lifecycleScope.launch {
            if (feedBanned) {
                // remove from feed bans
                val feedBanList = actionLists.first { it.action == "hide" && it.scope == "feed" }
                Network.perform(
                    networkAction = { Network.removeFromActionList(feedBanList, profile) },
                    uiAction = {
                        feedBanList.profiles.remove(profile)
                        feedBanToggle.setImageDrawable(accountFeedUnbanned)
                        Toast.makeText(activity, R.string.unbanned_from_feed, Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // add to feed bans
                val feedBanList = actionLists.firstOrNull { it.action == "hide" && it.scope == "feed" }
                val feedBanReq = ActionListRequest().apply {
                    kind = "profile"
                    action = "hide"
                    scope = "feed"
                    profiles.add(profile)
                }

                Network.perform(
                    networkAction = { Network.addToActionList(feedBanReq) },
                    uiAction = {
                        if (feedBanList != null) {
                            feedBanList.profiles.add(profile)
                        } else {
                            actionLists.add(ActionList().apply {
                                kind = "profile"
                                action = "hide"
                                scope = "feed"
                                profiles = HasMany(profile)
                            })
                        }
                        feedBanToggle.setImageDrawable(accountFeedBanned)
                        Toast.makeText(activity, R.string.banned_from_feed, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    @OnClick(R.id.author_ban)
    fun toggleBan() {
        val accBanned = actionLists.filter { it.action == "ban" && it.scope == "blog" }
                .flatMap { list -> list.profiles.get() }
                .any { prof -> prof.idMatches(profile) }

        lifecycleScope.launch {
            if (accBanned) {
                // remove from bans
                val banList = actionLists.first { it.action == "ban" && it.scope == "blog" }
                Network.perform(
                        networkAction = { Network.removeFromActionList(banList, profile) },
                        uiAction = {
                            banList.profiles.remove(profile)
                            banToggle.setImageDrawable(accountUnbanned)
                            Toast.makeText(activity, R.string.unbanned, Toast.LENGTH_SHORT).show()
                        }
                )
            } else {
                // add to bans
                val banList = actionLists.firstOrNull { it.action == "ban" && it.scope == "blog" }
                val banReq = ActionListRequest().apply {
                    kind = "profile"
                    action = "ban"
                    scope = "blog"
                    profiles.add(profile)
                }

                Network.perform(
                        networkAction = { Network.addToActionList(banReq) },
                        uiAction = {
                            if (banList != null) {
                                banList.profiles.add(profile)
                            } else {
                                actionLists.add(ActionList().apply {
                                    kind = "profile"
                                    action = "ban"
                                    scope = "blog"
                                    profiles = HasMany(profile)
                                })
                            }
                            banToggle.setImageDrawable(accountBanned)
                            Toast.makeText(activity, R.string.banned, Toast.LENGTH_SHORT).show()
                        }
                )
            }
        }
    }

    @OnClick(R.id.author_add_to_favorites)
    fun toggleFavorite() {
        lifecycleScope.launch {
            try {
                if (Auth.profile?.favorites?.any { it.idMatches(profile) } == true) {
                    // remove from favorites
                    withContext(Dispatchers.IO) { Network.removeFavorite(profile) }
                    Auth.profile?.favorites?.remove(profile)
                    favoritesToggle.setImageDrawable(accountUnfavorited)
                    Toast.makeText(activity, R.string.removed_from_favorites, Toast.LENGTH_SHORT).show()
                } else {
                    // add to favorites
                    withContext(Dispatchers.IO) { Network.addFavorite(profile) }
                    Auth.profile?.apply {
                        favorites.add(profile)
                        document.addInclude(profile)
                    }
                    favoritesToggle.setImageDrawable(accountFavorited)
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