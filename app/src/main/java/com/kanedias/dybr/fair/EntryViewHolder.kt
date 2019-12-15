package com.kanedias.dybr.fair

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.view.View
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
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.*
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.list.listItems
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.idMatches
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.openUrlExternally
import com.kanedias.dybr.fair.ui.showToastAtView
import com.kanedias.dybr.fair.misc.styleLevel
import kotlinx.coroutines.*

/**
 * View holder for showing regular entries in diary view.
 * @param iv inflated view to be used by this holder
 * @param allowSelection whether text in this view can be selected and copied
 * @see EntryListFragment.entryRibbon
 * @author Kanedias
 */
class EntryViewHolder(iv: View, private val parent: UserContentListFragment, private val allowSelection: Boolean = false) : UserContentViewHolder<Entry>(iv) {

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

    @BindViews(
            R.id.entry_bookmark, R.id.entry_subscribe, R.id.entry_edit,
            R.id.entry_delete, R.id.entry_more_options, R.id.entry_add_reaction
    )
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    @BindViews(R.id.entry_participants_indicator, R.id.entry_comments_indicator)
    lateinit var indicators: List<@JvmSuppressWildcards ImageView>

    @BindView(R.id.entry_comments_text)
    lateinit var comments: TextView

    @BindView(R.id.entry_participants_text)
    lateinit var participants: TextView

    @BindView(R.id.entry_permissions)
    lateinit var permissionIcon: ImageView

    @BindView(R.id.entry_pinned)
    lateinit var pinIcon: ImageView

    @BindView(R.id.entry_reactions)
    lateinit var reactionArea: LinearLayout

    /**
     * Entry that this holder represents
     */
    private lateinit var entry: Entry

    /**
     * Optional metadata associated with current entry
     */
    private var metadata: EntryMeta? = null
    private var reactions: MutableList<Reaction> = mutableListOf()

    /**
     * Blog this entry belongs to
     */
    private lateinit var profile: OwnProfile

    override fun getCreationDateView() = dateView
    override fun getProfileAvatarView() = avatarView
    override fun getAuthorNameView() = authorView
    override fun getContentView() = bodyView

    /**
     * Listener to show comments of this entry
     */
    private val commentShow  = View.OnClickListener {
        val activity = it.context as AppCompatActivity
        val commentsPage = CommentListFragment().apply { entry = this@EntryViewHolder.entry }
        activity.showFullscreenFragment(commentsPage)
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
        val styleLevel = parent.styleLevel

        styleLevel.bind(TEXT_BLOCK, itemView, CardViewColorAdapter())
        styleLevel.bind(TEXT_HEADERS, titleView)
        styleLevel.bind(TEXT, authorView)
        styleLevel.bind(TEXT, dateView)
        styleLevel.bind(TEXT, bodyView)
        styleLevel.bind(TEXT_LINKS, bodyView, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, tagsView, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, permissionIcon)
        styleLevel.bind(TEXT_LINKS, pinIcon)
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

        activity.showFullscreenFragment(entryEdit)
    }

    @OnClick(R.id.entry_subscribe)
    fun subscribeToEntry(button: ImageView) {
        val subscribe = !(metadata?.subscribed ?: false)

        val toastText = when (subscribe) {
            true -> R.string.subscribed_to_entry
            false -> R.string.unsubscribed_from_entry
        }

        parent.uiScope.launch(Dispatchers.Main) {
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

    @OnClick(R.id.entry_bookmark)
    fun bookmarkEntry(button: ImageView) {
        val bookmark = !(metadata?.bookmark ?: false)

        val toastText = when (bookmark) {
            true -> R.string.entry_bookmarked
            false -> R.string.entry_removed_from_bookmarks
        }

        parent.uiScope.launch(Dispatchers.Main) {
            try {
                withContext(Dispatchers.IO) { Network.updateBookmark(entry, bookmark) }
                showToastAtView(button, itemView.context.getString(toastText))

                metadata?.bookmark = bookmark
                setupButtons()
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }
    }

    @OnClick(R.id.entry_add_reaction)
    fun openReactionMenu(button: ImageView) {
        parent.uiScope.launch(Dispatchers.Main) {
            try {
                val reactionSets = withContext(Dispatchers.IO) { Network.loadReactionSets() }
                if (!reactionSets.isNullOrEmpty()) {
                    showReactionMenu(button, reactionSets.first())
                }
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }
    }

    private fun showReactionMenu(view: View, reactionSet: ReactionSet) {
        val reactionTypes = reactionSet.reactionTypes?.get(reactionSet.document).orEmpty()

        val emojiTable = View.inflate(view.context, R.layout.view_emoji_panel, null) as GridLayout
        val pw = PopupWindow().apply {
            height = WindowManager.LayoutParams.WRAP_CONTENT
            width = WindowManager.LayoutParams.WRAP_CONTENT
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // hack for Android 5.1.1, see https://stackoverflow.com/questions/12232724/popupwindow-dismiss-when-clicked-outside
            contentView = emojiTable
            isOutsideTouchable = true
        }

        for (type in reactionTypes.sortedBy { it.id }) {
            emojiTable.addView(TextView(view.context).apply {
                text = type.emoji
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setOnClickListener {
                    toggleReaction(view, type)
                    pw.dismiss()
                }
            })
        }
        pw.showAsDropDown(view, 0, 0, Gravity.TOP)
    }

    @OnClick(R.id.entry_delete)
    fun deleteEntry() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            parent.uiScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.deleteEntry(entry) }
                    Toast.makeText(activity, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    parent.loadMore(reset = true)
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
        val items = mutableListOf(
                ctx.getString(R.string.open_in_browser),
                ctx.getString(R.string.share)
        )

        if (parent is EntryListFragment && parent.profile === Auth.worldMarker) {
            // show hide-from-feed option
            items.add(ctx.getString(R.string.hide_author_from_feed))
        }

        MaterialDialog(itemView.context)
                .title(R.string.entry_menu)
                .listItems(items = items, selection = {_, index, _ ->  when (index) {
                    0 -> showInWebView()
                    1 -> sharePost()
                    2 -> hideFromFeed()
                }}).show()
    }

    private fun hideFromFeed() {
        // hide callback
        val hide = {
            val activity = itemView.context as AppCompatActivity

            val listItem = ActionListRequest().apply {
                scope = "feed"
                profiles.add(profile)
            }

            parent.uiScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.createActionList(listItem) }
                    Toast.makeText(activity, R.string.author_hidden_from_feed, Toast.LENGTH_SHORT).show()
                    parent.loadMore(reset = true)
                } catch (ex: Exception) {
                    Network.reportErrors(itemView.context, ex)
                }
            }
        }

        MaterialDialog(itemView.context)
                .title(R.string.confirm_action)
                .message(R.string.are_you_sure)
                .negativeButton(android.R.string.no)
                .positiveButton(android.R.string.yes, click = { hide() })
                .show()
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

        // setup bookmark button
        val bookmarkButton = buttons.first { it.id == R.id.entry_bookmark }
        when (metadata?.bookmark) {
            true -> bookmarkButton.apply { visibility = View.VISIBLE; setImageResource(R.drawable.bookmark_filled) }
            false -> bookmarkButton.apply { visibility = View.VISIBLE; setImageResource(R.drawable.bookmark_add) }
            null -> bookmarkButton.visibility = View.GONE
        }

        // setup reactions button
        val reactionButton = buttons.first { it.id == R.id.entry_add_reaction }
        when {
            // disabled globally by current user
            Auth.profile?.settings?.reactions?.disable == true -> reactionButton.visibility = View.GONE
            // disabled in current blog by owner
            profile.settings.reactions.disableInBlog -> reactionButton.visibility = View.GONE
            // enabled, show the button
            else -> reactionButton.visibility = View.VISIBLE
        }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    override fun setup(entity: Entry) {
        super.setup(entity)

        // bind variables
        this.entry = entity
        this.metadata = Network.bufferToObject<EntryMeta>(entry.meta)
        this.profile = entity.profile.get(entity.document)
        this.reactions = entity.reactions?.get(entity.document) ?: mutableListOf()

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

        // setup pin icon
        val pinned = profile.settings.pinnedEntries.contains(entry.id)
        if (pinned) {
            pinIcon.visibility = View.VISIBLE
            pinIcon.setOnClickListener { showToastAtView(pinIcon, it.context.getString(R.string.pinned_entry)) }
        } else {
            pinIcon.visibility = View.GONE
        }

        // show tags if they are present
        setupTags(entry)

        // setup bottom row of metadata buttons
        metadata?.let { comments.text = it.comments.toString() }
        metadata?.let { participants.text = it.commenters.toString() }

        // setup bottom row of buttons
        setupButtons()

        // setup reaction row
        setupReactions()

        // don't show subscribe button if we can't subscribe
        // guests can't do anything
        if (Auth.profile == null) {
            buttons.first { it.id == R.id.entry_subscribe }.visibility = View.GONE
            buttons.first { it.id == R.id.entry_bookmark }.visibility = View.GONE
        }

        bodyView.handleMarkdown(entry.content)
    }

    /**
     * Setup reactions row, to show reactions which were attached to this entry
     */
    private fun setupReactions() {
        reactionArea.removeAllViews()

        val reactionsDisabled = Auth.profile?.settings?.reactions?.disable == true
        val reactionsDisabledInThisBlog = profile.settings.reactions.disableInBlog

        if (reactions.isEmpty() || reactionsDisabled || reactionsDisabledInThisBlog) {
            // no reactions for this entry or reactions disabled
            reactionArea.visibility = View.GONE
            return
        } else {
            reactionArea.visibility = View.VISIBLE
        }

        // there are some reactions, display them
        val styleLevel = parent.styleLevel
        val counts = reactions.groupBy { it.reactionType.get().id }
        val types = reactions.map { it.reactionType.get(it.document) }.associateBy { it.id }
        for (reactionTypeId in counts.keys) {
            // for each reaction type get reaction counts and authors
            val reactionType = types[reactionTypeId] ?: continue
            val postedWithThisType = counts[reactionTypeId] ?: continue
            val includingMe = postedWithThisType.any { Auth.profile?.idMatches(it.author.get()) == true }

            val reactionView = LayoutInflater.from(itemView.context).inflate(R.layout.view_reaction, reactionArea, false)

            reactionView.setOnClickListener { toggleReaction(it, reactionType) }
            if (includingMe) {
                reactionView.isSelected = true
            }

            val emojiTxt = reactionView.findViewById<TextView>(R.id.reaction_emoji)
            val emojiCount = reactionView.findViewById<TextView>(R.id.reaction_count)

            emojiTxt.text = reactionType.emoji
            emojiCount.text = postedWithThisType.size.toString()

            styleLevel.bind(TEXT_LINKS, reactionView, BackgroundTintColorAdapter())
            styleLevel.bind(TEXT, emojiCount)

            reactionArea.addView(reactionView)
        }
    }

    private fun toggleReaction(view: View, reactionType: ReactionType) {
        // find reaction with this type
        val myReaction = reactions
                .filter { reactionType.idMatches(it.reactionType.get()) }
                .find { Auth.profile?.idMatches(it.author.get()) == true }

        if (myReaction != null) {
            // it's there, delete it
            parent.uiScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.deleteReaction(myReaction) }
                    showToastAtView(view, view.context.getString(R.string.reaction_deleted))

                    reactions.remove(myReaction)
                    setupReactions()
                } catch (ex: Exception) {
                    Network.reportErrors(view.context, ex)
                }
            }
        } else {
            // add it
            parent.uiScope.launch(Dispatchers.Main) {
                try {
                    val newReaction = withContext(Dispatchers.IO) { Network.createReaction(entry, reactionType) }
                    showToastAtView(view, view.context.getString(R.string.reaction_added))

                    reactions.add(newReaction)
                    setupReactions()
                } catch (ex: Exception) {
                    Network.reportErrors(view.context, ex)
                }
            }
        }
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
                    putSerializable("filters", hashMapOf(
                            "tag" to tagValue,
                            "profile_id" to this@EntryViewHolder.profile.id)
                    )
                }
            }
            activity.showFullscreenFragment(searchFragment)
        }

        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }

}