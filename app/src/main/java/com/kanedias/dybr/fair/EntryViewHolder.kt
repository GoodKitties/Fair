package com.kanedias.dybr.fair

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.ui.handleMarkdown
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.afollestad.materialdialogs.list.listItems
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.openUrlExternally
import com.kanedias.dybr.fair.ui.showToastAtView
import com.kanedias.dybr.fair.ui.styleLevel
import kotlinx.coroutines.*

/**
 * View holder for showing regular entries in diary view.
 * @param iv inflated view to be used by this holder
 * @param allowSelection whether text in this view can be selected and copied
 * @see EntryListFragment.entryRibbon
 * @author Kanedias
 */
class EntryViewHolder(iv: View, private val parent: View, private val allowSelection: Boolean = false) : UserContentViewHolder<Entry>(iv) {

    @BindView(R.id.entry_avatar)
    lateinit var avatarView: ImageView

    @BindView(R.id.entry_author)
    lateinit var authorView: TextView

    @BindView(R.id.entry_title)
    lateinit var titleView: TextView

    @BindView(R.id.entry_date)
    lateinit var dateView: TextView

    @BindView(R.id.entry_message)
    lateinit var bodyView: TextView

    @BindView(R.id.entry_tags)
    lateinit var tagsView: TextView

    @BindView(R.id.entry_meta_divider)
    lateinit var metaDivider: View

    @BindView(R.id.entry_draft_state)
    lateinit var draftStateView: TextView

    @BindViews(R.id.entry_subscribe, R.id.entry_edit, R.id.entry_delete, R.id.entry_more_options)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    @BindViews(R.id.entry_participants_indicator, R.id.entry_comments_indicator)
    lateinit var indicators: List<@JvmSuppressWildcards ImageView>

    @BindView(R.id.entry_comments_text)
    lateinit var comments: TextView

    @BindView(R.id.entry_participants_text)
    lateinit var participants: TextView

    @BindView(R.id.entry_permissions)
    lateinit var permissionIcon: ImageView

    /**
     * Entry that this holder represents
     */
    private lateinit var entry: Entry

    /**
     * Optional metadata associated with current entry
     */
    private var metadata: EntryMeta? = null

    /**
     * Blog this entry belongs to
     */
    private lateinit var profile: OwnProfile

    override fun getCreationDateView() = dateView
    override fun getProfileAvatarView() = avatarView
    override fun getAuthorNameView() = authorView

    /**
     * Listener to show comments of this entry
     */
    private val commentShow  = View.OnClickListener {
        val activity = it.context as AppCompatActivity
        val commentsPage = CommentListFragment().apply { entry = this@EntryViewHolder.entry }
        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing comment list fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, commentsPage)
                .commit()
    }

    init {
        ButterKnife.bind(this, iv)
        setupTheming()

        tagsView.movementMethod = LinkMovementMethod()
        iv.setOnClickListener(commentShow)
        if (allowSelection) {
            bodyView.isLongClickable = true
        }
    }

    private fun setupTheming() {
        val styleLevel = parent.styleLevel ?: return

        styleLevel.bind(TEXT_BLOCK, itemView, CardViewColorAdapter())
        styleLevel.bind(TEXT_HEADERS, titleView)
        styleLevel.bind(TEXT, authorView)
        styleLevel.bind(TEXT, dateView)
        styleLevel.bind(TEXT, bodyView)
        styleLevel.bind(TEXT_LINKS, bodyView, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, tagsView, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, permissionIcon)
        styleLevel.bind(DIVIDER, metaDivider)
        (buttons + indicators + participants + comments).forEach { styleLevel.bind(TEXT_LINKS, it) }
    }

    @OnClick(R.id.entry_edit)
    fun editEntry() {
        val activity = itemView.context as AppCompatActivity
        val entryEdit = CreateNewEntryFragment().apply {
            profile = this@EntryViewHolder.profile
            editMode = true
            editEntry = this@EntryViewHolder.entry
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing entry edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, entryEdit)
                .commit()
    }

    @OnClick(R.id.entry_subscribe)
    fun subscribeToEntry(button: ImageView) {
        val subscribe = !(metadata?.subscribed ?: false)

        val toastText = when (subscribe) {
            true -> R.string.subscribed_to_entry
            false -> R.string.unsubscribed_from_entry
        }

        GlobalScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) { Network.updateSubscription(entry, subscribe) }
                showToastAtView(button, itemView.context.getString(toastText))

                metadata?.subscribed = subscribe
                setupButtons()
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }
    }

    @OnClick(R.id.entry_delete)
    fun deleteEntry() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.deleteEntry(entry) }
                    Toast.makeText(activity, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    // if we have current tab, refresh it
                    val plPredicate = { it: Fragment -> it is EntryListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(plPredicate) as EntryListFragment?
                    currentTab?.loadMore(reset = true)
                } catch (ex: Exception) {
                    Network.reportErrors(itemView.context, ex)
                }
            }
        }

        // show confirmation dialog
        MaterialDialog(itemView.context)
                .title(R.string.confirm_action)
                .message(R.string.are_you_sure)
                .negativeButton(android.R.string.no)
                .positiveButton(android.R.string.yes, click = { delete() })
                .show()
    }

    @OnClick(R.id.entry_more_options)
    fun showOverflowMenu() {
        val ctx = itemView.context
        val items = listOf(
                ctx.getString(R.string.open_in_browser),
                ctx.getString(R.string.share)
                )

        MaterialDialog(itemView.context)
                .title(R.string.entry_menu)
                .listItems(items = items, selection = {_, index, _ ->  when (index) {
                    0 -> showInWebView()
                    1 -> sharePost()
                }}).show()
    }

    private fun sharePost() {
        val ctx = itemView.context

        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, "https://dybr.ru/blog/${profile.blogSlug}/${entry.id}")
            ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.share_link_using)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, R.string.no_browser_found, Toast.LENGTH_SHORT).show()
        }

    }

    private fun showInWebView() {
        val uri = Uri.Builder()
                .scheme("https").authority("dybr.ru")
                .appendPath("blog").appendPath(profile.blogSlug)
                .appendPath(entry.id)
                .build()

        openUrlExternally(itemView.context, uri)
    }

    /**
     * Show or hide entry editing buttons depending on circumstances
     */
    private fun setupButtons() {
        // setup edit buttons
        val editVisibility = when (isBlogWritable(profile)) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        val editTag = itemView.context.getString(R.string.edit_tag)
        buttons.filter { it.tag == editTag }.forEach { it.visibility = editVisibility }

        // setup subscription button
        val subButton = buttons.first { it.id == R.id.entry_subscribe }
        when (metadata?.subscribed) {
            true -> subButton.apply { visibility = View.VISIBLE; setImageResource(R.drawable.star_filled) }
            false -> subButton.apply { visibility = View.VISIBLE; setImageResource(R.drawable.star_border) }
            null -> subButton.visibility = View.GONE
        }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    override fun setup(entity: Entry, standalone: Boolean) {
        super.setup(entity, standalone)

        // bind variables
        this.entry = entity
        this.metadata = Network.bufferToObject<EntryMeta>(entry.meta)
        this.profile = entity.profile.get(entity.document)

        // setup text views from entry data
        titleView.text = entry.title
        draftStateView.visibility = if (entry.state == "published") { View.GONE } else { View.VISIBLE }

        // setup permission icon
        val accessItem = entry.settings?.permissions?.access?.firstOrNull()
        if (accessItem == null) {
            permissionIcon.visibility = View.GONE
        } else {
            permissionIcon.visibility = View.VISIBLE
            permissionIcon.setOnClickListener { showToastAtView(permissionIcon, accessItem.toDescription(it.context)) }
        }

        // show tags if they are present
        setupTags(entry)

        // setup bottom row of metadata buttons
        metadata?.let { comments.text = it.comments.toString() }
        metadata?.let { participants.text = it.commenters.toString() }

        // setup bottom row of buttons
        setupButtons()

        // hack
        if (!standalone) {
            buttons.first { it.id == R.id.entry_subscribe }.visibility = View.GONE
        }

        bodyView.handleMarkdown(entry.content)
    }

    /**
     * Show tags below the message, with divider.
     * Make tags below the entry message clickable.
     */
    private fun setupTags(entry: Entry) {
        if (entry.tags.isEmpty()) {
            tagsView.visibility = View.GONE
        } else {
            tagsView.visibility = View.VISIBLE

            val clickTags = SpannableStringBuilder()
            for (tag in entry.tags) {
                clickTags.append("#").append(tag)
                clickTags.setSpan(ClickableTag(tag), clickTags.length - 1 - tag.length, clickTags.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                clickTags.append(" ")
            }
            tagsView.text = clickTags
        }
    }

    /**
     * Clickable tag span. Don't make it look like a URL link but make it clickable nevertheless.
     */
    inner class ClickableTag(private val tagValue: String): ClickableSpan() {

        override fun onClick(widget: View) {
            val activity = itemView.context as AppCompatActivity

            val searchFragment = EntryListSearchTagFragmentFull().apply {
                arguments = Bundle().apply {
                    putSerializable("filters", hashMapOf("tag" to tagValue))
                }
            }
            activity.supportFragmentManager.beginTransaction()
                    .addToBackStack("Showing search tag fragment after click to tag")
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .add(R.id.main_drawer_layout, searchFragment)
                    .commit()
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }

}