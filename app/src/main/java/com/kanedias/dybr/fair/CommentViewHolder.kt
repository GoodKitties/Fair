package com.kanedias.dybr.fair

import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.handleMarkdown
import kotlinx.coroutines.*


/**
 * View holder for showing comments in entry view. Has quick buttons for editing actions if this
 * comment is yours.
 *
 * @param parentFragment fragment that this view holder belongs to. Needed for styling and
 *                       proper async job lifecycle
 * @param entry entry that this comment is posted to
 * @see CommentListFragment.commentRibbon
 * @author Kanedias
 */
class CommentViewHolder(iv: View, parentFragment: UserContentListFragment) : UserContentViewHolder<Comment>(iv, parentFragment) {

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
    }

    override fun getCreationDateView() = dateView
    override fun getProfileAvatarView() = avatarView
    override fun getAuthorNameView() = authorView
    override fun getContentView() = bodyView

    private fun setupTheming() {
        val styleLevel = parentFragment.styleLevel

        styleLevel.bind(TEXT_BLOCK, itemView, CardViewColorAdapter())
        styleLevel.bind(TEXT, authorView)
        styleLevel.bind(TEXT, dateView)
        styleLevel.bind(TEXT, bodyView)
        styleLevel.bind(TEXT_LINKS, bodyView, TextViewLinksAdapter())
        buttons.forEach { styleLevel.bind(TEXT_LINKS, it) }
    }

    @OnClick(R.id.comment_edit)
    fun editComment() {
        val activity = itemView.context as AppCompatActivity
        val parentEntry = comment.entry.get(comment.document)
        val commentEdit = CreateNewCommentFragment().apply {
            entry = parentEntry
            editComment = this@CommentViewHolder.comment
            editMode = true
        }

        activity.showFullscreenFragment(commentEdit)
    }

    @OnClick(R.id.comment_delete)
    fun deleteComment() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            parentFragment.uiScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.deleteComment(comment) }
                    Toast.makeText(activity, R.string.comment_deleted, Toast.LENGTH_SHORT).show()

                    // if we have current tab, refresh it
                    val clPredicate = { it: Fragment -> it is CommentListFragment && it.lifecycle.currentState == Lifecycle.State.RESUMED }
                    val currentTab = activity.supportFragmentManager.fragments.find(clPredicate) as CommentListFragment?
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
    override fun setup(entity: Comment) {
        super.setup(entity)

        this.comment = entity

        bodyView.handleMarkdown(comment.content)

        val profile = comment.profile.get(comment.document)
        toggleEditButtons(isBlogWritable(profile))

        // make text selectable
        // XXX: this is MAGIC: see https://stackoverflow.com/a/56224791/1696844
        bodyView.setTextIsSelectable(false)
        bodyView.measure(-1, -1)
        bodyView.setTextIsSelectable(true)
    }


}