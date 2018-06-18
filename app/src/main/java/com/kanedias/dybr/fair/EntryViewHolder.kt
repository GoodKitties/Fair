package com.kanedias.dybr.fair

import android.app.FragmentTransaction
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
import com.kanedias.dybr.fair.entities.Entry
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*


/**
 * View holder for showing regular entries in diary view.
 * @param iv inflated view to be used by this holder
 * @param allowSelection whether text in this view can be selected and copied
 * @see EntryListFragment.entryRibbon
 * @author Kanedias
 */
class EntryViewHolder(iv: View, private val allowSelection: Boolean = false) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.entry_author)
    lateinit var authorView: TextView

    @BindView(R.id.entry_title)
    lateinit var titleView: TextView

    @BindView(R.id.entry_date)
    lateinit var dateView: TextView

    @BindView(R.id.entry_message)
    lateinit var bodyView: TextView

    @BindView(R.id.entry_draft_state)
    lateinit var draftStateView: TextView

    @BindViews(R.id.entry_edit, R.id.entry_delete, R.id.entry_more_options)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

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
                .replace(R.id.main_drawer_layout, commentsPage)
                .commit()
    }

    init {
        ButterKnife.bind(this, iv)

        iv.setOnClickListener(commentShow)
        if (allowSelection) {
            bodyView.isLongClickable = true
        }
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
                .replace(R.id.main_drawer_layout, entryEdit)
                .commit()
    }

    @OnClick(R.id.entry_delete)
    fun deleteEntry() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            launch(UI) {
                try {
                    async(CommonPool) { Network.deleteEntry(entry) }.await()
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
                .onPositive({ _, _ -> delete() })
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
                        1 -> showInWebView()
                    }
                }.show()
    }

    private fun showInWebView() {

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
        val metadata = Network.bufferToMap(entry.meta)
        metadata["comments"]?.let { comments.text = it }
        metadata["commenters"]?.let { participants.text = it }

        bodyView.handleMarkdown(entry.content)
    }
}