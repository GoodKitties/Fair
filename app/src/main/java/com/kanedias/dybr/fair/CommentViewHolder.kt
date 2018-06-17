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
import com.kanedias.dybr.fair.entities.*
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.text.SimpleDateFormat
import java.util.*


/**
 * View holder for showing comments in entry view.
 *
 * @see CommentListFragment.commentRibbon
 * @author Kanedias
 */
class CommentViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

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

        bodyView.setTextIsSelectable(true)
        bodyView.isLongClickable = true
    }

    @OnClick(R.id.comment_edit)
    fun editComment() {
        val activity = itemView.context as AppCompatActivity
        val commentEdit = CreateNewCommentFragment().apply {
            editMode = true
            editComment = this@CommentViewHolder.comment
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing comment edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, commentEdit)
                .commit()
    }

    @OnClick(R.id.comment_delete)
    fun deleteComment() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            launch(UI) {
                try {
                    async { Network.deleteComment(comment) }.await()
                    Toast.makeText(activity, R.string.comment_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    // if we have current tab, refresh it
                    val plPredicate = { it: Fragment -> it is CommentListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(plPredicate) as CommentListFragment?
                    currentTab?.refreshComments()
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
    fun setup(comment: Comment, profile: OwnProfile) {
        this.comment = comment

        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(comment.createdAt)
        authorView.text = profile.nickname
        bodyView.handleMarkdown(comment.content)

        // time diff must be lower than 15 minutes
        val timeDiff = (Date().time - comment.createdAt.time) / 1000 // in seconds
        toggleEditButtons(profile == Auth.profile && timeDiff < 60 * 15)
    }
}