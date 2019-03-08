package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.text.format.DateUtils
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.kanedias.dybr.fair.dto.Authored
import com.kanedias.dybr.fair.ui.getTopFragment
import com.kanedias.dybr.fair.ui.showToastAtView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Holder that shows info and interactive elements for any authored entity.
 * Has convenient handlers for date showing, author dialog popup, author name click.
 *
 * @author Kanedias
 *
 * Created on 04.01.19
 */
abstract class UserContentViewHolder<T: Authored>(iv: View): RecyclerView.ViewHolder(iv) {

    abstract fun getCreationDateView(): TextView
    abstract fun getAuthorNameView(): TextView
    abstract fun getProfileAvatarView(): ImageView
    abstract fun getContentView(): TextView

    open fun setup(entity: T, standalone: Boolean = false) {
        val profile = entity.profile.get(entity.document)

        getCreationDateView().text = DateUtils.getRelativeTimeSpanString(entity.createdAt.time)
        getCreationDateView().setOnClickListener { showFullDate(entity) }

        val avatar = profile.settings?.avatar
        if (avatar != null && HttpUrl.parse(avatar) != null) {
            Glide.with(getProfileAvatarView()).load(avatar)
                    .apply(RequestOptions().downsample(DownsampleStrategy.CENTER_INSIDE))
                    .into(getProfileAvatarView())
        } else {
            getProfileAvatarView().setImageDrawable(null)
        }
        getProfileAvatarView().setOnClickListener { showProfile(entity) }

        getAuthorNameView().text = profile.nickname
        getAuthorNameView().setOnClickListener { replyWithName(entity) }

        getContentView().customSelectionActionModeCallback = SelectionEnhancer(entity)
    }

    /**
     * Called when user clicks on relative date text field.
     * Shows full date listing in a toast.
     *
     * @param entity entity to use created date from
     */
    private fun showFullDate(entity: T) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entity.createdAt.time)
        showToastAtView(getCreationDateView(), date)
    }

    /**
     * Called when user clicks on author avatar.
     * Shows dialog with author's profile.
     *
     * @param entity entity to show author from
     */
    private fun showProfile(entity: T) {
        val activity = itemView.context as AppCompatActivity
        val partialProf = entity.profile.get(entity.document)

        val dialog = MaterialDialog(activity)
                .cancelable(false)
                .title(R.string.please_wait)
                .message(R.string.loading_profile)

        GlobalScope.launch(Dispatchers.Main) {
            dialog.show()

            try {
                val prof = withContext(Dispatchers.IO) { Network.loadProfile(partialProf.id) }
                val profShow = ProfileFragment().apply { profile = prof }
                profShow.show(activity.supportFragmentManager, "Showing user profile fragment")
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }

            dialog.dismiss()
        }
    }

    /**
     * Called when user clicks an author's name  on comments page.
     * Opens new create comment fragment and inserts link to the author.
     *
     * @param entity entity to reply to
     */
    private fun replyWithName(entity: T) {
        val activity = itemView.context as AppCompatActivity
        val commentList = activity.getTopFragment(CommentListFragment::class) ?: return

        // open create new comment fragment and insert nickname
        val commentAdd = CreateNewCommentFragment().apply {
            this.entry = commentList.entry!!
            arguments = Bundle().apply { putSerializable(CreateNewCommentFragment.AUTHOR_LINK, entity) }
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing comment add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, commentAdd)
                .commit()
    }

    inner class SelectionEnhancer(private val entity: T): ActionMode.Callback {

        private val textView = getContentView()

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            val text = textView.text.subSequence(textView.selectionStart, textView.selectionEnd)

            when(item.itemId) {
                R.id.menu_reply -> {
                    val activity = itemView.context as AppCompatActivity
                    val commentList = activity.getTopFragment(CommentListFragment::class) ?: return true

                    val commentAdd = CreateNewCommentFragment().apply {
                        this.entry = commentList.entry!!
                        arguments = Bundle().apply {
                            putSerializable(CreateNewCommentFragment.AUTHOR_LINK, entity)
                            putSerializable(CreateNewCommentFragment.REPLY_TEXT, text.toString())
                        }
                    }

                    activity.supportFragmentManager.beginTransaction()
                            .addToBackStack("Showing comment add fragment")
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .add(R.id.main_drawer_layout, commentAdd)
                            .commit()

                    return true
                }
                else -> return false
            }
        }

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.content_selection_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false

        override fun onDestroyActionMode(mode: ActionMode) = Unit
    }
}