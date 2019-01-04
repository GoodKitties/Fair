package com.kanedias.dybr.fair

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import kotlinx.coroutines.*


/**
 * View holder for showing comments in entry view.
 *
 * @see CommentListFragment.commentRibbon
 * @author Kanedias
 */
class CommentViewHolder(private val entry: Entry, iv: View, private val parent: View) : UserContentViewHolder(iv) {

    @BindView(R.id.comment_avatar)
    lateinit var avatarView: ImageView

    @BindView(R.id.comment_date)
    lateinit var dateView: TextView

    @BindView(R.id.comment_author)
    lateinit var authorView: TextView

    @BindView(R.id.comment_message)
    lateinit var bodyView: TextView

    @BindViews(R.id.comment_edit, R.id.comment_delete)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    private lateinit var comment: Comment

    init {
        ButterKnife.bind(this, iv)
        setupTheming()

        // make text selectable
        bodyView.isLongClickable = true
    }

    override fun getCreationDateView() = dateView
    override fun getProfileAvatarView() = avatarView
    override fun getAuthorNameView() = authorView

    private fun setupTheming() {
        Scoop.getInstance().bind(TEXT_BLOCK, itemView, parent, CardViewColorAdapter())
        Scoop.getInstance().bind(TEXT, authorView, parent)
        Scoop.getInstance().bind(TEXT, dateView, parent)
        Scoop.getInstance().bind(TEXT, bodyView, parent)
        Scoop.getInstance().bind(TEXT_LINKS, bodyView, parent, TextViewLinksAdapter())
        buttons.forEach { Scoop.getInstance().bind(TEXT_LINKS, it, parent) }
    }

    @OnClick(R.id.comment_edit)
    fun editComment() {
        val activity = itemView.context as AppCompatActivity
        val commentEdit = CreateNewCommentFragment().apply {
            entry = this@CommentViewHolder.entry
            editMode = true
            editComment = this@CommentViewHolder.comment
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing comment edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, commentEdit)
                .commit()
    }

    @OnClick(R.id.comment_delete)
    fun deleteComment() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.deleteComment(comment) }
                    Toast.makeText(activity, R.string.comment_deleted, Toast.LENGTH_SHORT).show()

                    // if we have current tab, refresh it
                    val clPredicate = { it: Fragment -> it is CommentListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(clPredicate) as CommentListFragment?
                    currentTab?.refreshComments(reset = true)
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
    fun setup(comment: Comment) {
        super.setup(comment)

        this.comment = comment
        bodyView.handleMarkdown(comment.content)

        val profile = comment.profile.get(comment.document)
        toggleEditButtons(profile == Auth.profile)
    }
}