package com.kanedias.dybr.fair

import android.app.FragmentTransaction
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.Entry
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*
import android.content.ComponentName
import android.content.pm.PackageManager
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.EntryMeta
import com.kanedias.dybr.fair.themes.*

/**
 * View holder for showing regular entries in diary view.
 * @param iv inflated view to be used by this holder
 * @param allowSelection whether text in this view can be selected and copied
 * @see EntryListFragment.entryRibbon
 * @author Kanedias
 */
class EntryViewHolder(iv: View, private val parent: View, private val allowSelection: Boolean = false) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.entry_author)
    lateinit var authorView: TextView

    @BindView(R.id.entry_title)
    lateinit var titleView: TextView

    @BindView(R.id.entry_date)
    lateinit var dateView: TextView

    @BindView(R.id.entry_message)
    lateinit var bodyView: TextView

    @BindView(R.id.entry_divider)
    lateinit var divider: View

    @BindView(R.id.entry_draft_state)
    lateinit var draftStateView: TextView

    @BindViews(R.id.entry_edit, R.id.entry_delete, R.id.entry_more_options)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    @BindViews(R.id.entry_participants_indicator, R.id.entry_comments_indicator)
    lateinit var indicators: List<@JvmSuppressWildcards ImageView>

    @BindView(R.id.entry_comments_text)
    lateinit var comments: TextView

    @BindView(R.id.entry_participants_text)
    lateinit var participants: TextView

    /**
     * Entry that this holder represents
     */
    private lateinit var entry: Entry

    /**
     * Listener to show comments of this entry
     */
    private val commentShow  = View.OnClickListener { it ->
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

        iv.setOnClickListener(commentShow)
        if (allowSelection) {
            bodyView.isLongClickable = true
        }
    }

    private fun setupTheming() {
        Scoop.getInstance().bind(TEXT_BLOCK, itemView, parent, CardViewColorAdapter())
        Scoop.getInstance().bind(TEXT_HEADERS, titleView, parent)
        Scoop.getInstance().bind(TEXT, authorView, parent)
        Scoop.getInstance().bind(TEXT, dateView, parent)
        Scoop.getInstance().bind(TEXT, bodyView, parent)
        Scoop.getInstance().bind(TEXT_LINKS, bodyView, parent, TextViewLinksAdapter())
        Scoop.getInstance().bind(DIVIDER, divider, parent)
        (buttons + indicators + participants + comments).forEach { Scoop.getInstance().bind(TEXT_LINKS, it, parent) }
    }

    @OnClick(R.id.entry_edit)
    fun editEntry() {
        val activity = itemView.context as AppCompatActivity
        val entryEdit = CreateNewEntryFragment().apply {
            editMode = true
            editEntry = this@EntryViewHolder.entry
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing entry edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, entryEdit)
                .commit()
    }

    @OnClick(R.id.entry_delete)
    fun deleteEntry() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            launch(UI) {
                try {
                    async { Network.deleteEntry(entry) }.await()
                    Toast.makeText(activity, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    // if we have current tab, refresh it
                    val plPredicate = { it: Fragment -> it is EntryListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(plPredicate) as EntryListFragment?
                    currentTab?.refreshEntries(true)
                } catch (ex: Exception) {
                    Network.reportErrors(itemView.context, ex)
                }
            }
        }

        // show confirmation dialog
        MaterialDialog.Builder(itemView.context)
                .title(R.string.confirm_action)
                .content(R.string.are_you_sure)
                .negativeText(android.R.string.no)
                .positiveText(android.R.string.yes)
                .positiveColorRes(R.color.md_red_800)
                .onPositive { _, _ -> delete() }
                .show()
    }

    @OnClick(R.id.entry_more_options)
    fun showOverflowMenu() {
        val ctx = itemView.context
        val items = listOf(ctx.getString(R.string.show_web_version))

        MaterialDialog.Builder(itemView.context)
                .title(R.string.entry_menu)
                .items(items)
                .itemsCallback { _, _, position, _ ->
                    when (position) {
                        0 -> showInWebView()
                    }
                }.show()
    }

    @OnClick(R.id.entry_author)
    fun showAuthorProfile() {
        val activity = itemView.context as AppCompatActivity

        val dialog = MaterialDialog.Builder(activity)
                .progress(true, 0)
                .cancelable(false)
                .title(R.string.please_wait)
                .content(R.string.loading_profile)
                .build()

        launch(UI) {
            dialog.show()

            try {
                val prof = async { Network.loadProfile(entry.profile.get().id) }.await()
                val profShow = ProfileFragment().apply { profile = prof }
                profShow.show(activity.supportFragmentManager, "Showing user profile fragment")
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }

            dialog.dismiss()
        }
    }

    private fun showInWebView() {
        val blog = entry.blog.get(entry.document)
        val uri = Uri.Builder()
                .scheme("https").authority("dybr.ru")
                .appendPath("blog").appendPath(blog.slug)
                .appendPath(entry.id)
                .build()

        openUrlExternally(uri)
    }

    /**
     * Open the URL using the default browser on this device
     */
    private fun openUrlExternally(uri: Uri) {
        val ctx = itemView.context
        val pkgMgr = ctx.packageManager
        val intent = Intent(Intent.ACTION_VIEW, uri)

        // detect default browser
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://"))
        val defaultBrowser = pkgMgr.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)

        // use default browser to open the url
        intent.component = with(defaultBrowser.activityInfo) { ComponentName(applicationInfo.packageName, name) }
        ctx.startActivity(intent)
    }

    /**
     * Show or hide entry editing buttons depending on circumstances
     */
    private fun toggleEditButtons(show: Boolean) {
        val visibility = when (show) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        val editTag = itemView.context.getString(R.string.edit_tag)
        buttons.filter { it.tag == editTag }.forEach { it.visibility = visibility }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    fun setup(entry: Entry, editable: Boolean) {
        this.entry = entry

        // setup text views from entry data
        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entry.createdAt)
        titleView.text = entry.title
        entry.profile.get(entry.document)?.let { authorView.text = it.nickname }
        draftStateView.visibility = if (entry.state == "published") { View.GONE } else { View.VISIBLE }

        // setup bottom row of edit buttons
        toggleEditButtons(editable)

        // setup bottom row of metadata buttons
        val metadata = Network.bufferToObject<EntryMeta>(entry.meta)
        metadata?.let { comments.text = it.comments.toString() }
        metadata?.let { participants.text = it.commenters.toString() }

        bodyView.handleMarkdown(entry.content)
    }
}