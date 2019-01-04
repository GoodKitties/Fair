package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.kanedias.dybr.fair.dto.Authored
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

/**
 * Holder that shows info and interactive elements for any authored entity
 *
 * @author Kanedias
 *
 * Created on 04.01.19
 */
abstract class UserContentViewHolder(iv: View): RecyclerView.ViewHolder(iv) {

    abstract fun getCreationDateView(): TextView
    abstract fun getAuthorNameView(): TextView
    abstract fun getProfileAvatarView(): ImageView

    open fun setup(entity: Authored) {
        val profile = entity.profile.get(entity.document)

        getCreationDateView().text = DateUtils.getRelativeTimeSpanString(entity.createdAt.time)
        getCreationDateView().setOnClickListener { showFullDate(entity) }

        val avatar = profile.settings?.avatar
        if (avatar != null && HttpUrl.parse(avatar) != null) {
            Glide.with(getProfileAvatarView()).load(avatar)
                    .apply(RequestOptions().downsample(DownsampleStrategy.CENTER_INSIDE))
                    .into(getProfileAvatarView())
        }
        getProfileAvatarView().setOnClickListener { showProfile(entity) }

        getAuthorNameView().text = profile.nickname
        getAuthorNameView().setOnClickListener { replyWithName(entity) }
    }

    /**
     * Called when user clicks on relative date text field.
     * Shows full date listing in a toast.
     *
     * @param entity entity to use created date from
     */
    private fun showFullDate(entity: Authored) {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entity.createdAt.time)
        val toast = Toast.makeText(itemView.context, date, Toast.LENGTH_SHORT)

        val location = IntArray(2)
        getCreationDateView().getLocationOnScreen(location)

        toast.setGravity(Gravity.TOP or Gravity.START, getCreationDateView().right - 25, location[1] - 10)
        toast.show()
    }

    /**
     * Called when user clicks on author avatar.
     * Shows dialog with author's profile.
     *
     * @param entity entity to show author from
     */
    private fun showProfile(entity: Authored) {
        val activity = itemView.context as AppCompatActivity
        val partialProf = entity.profile.get(entity.document)

        val dialog = MaterialDialog.Builder(activity)
                .progress(true, 0)
                .cancelable(false)
                .title(R.string.please_wait)
                .content(R.string.loading_profile)
                .build()

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
    private fun replyWithName(entity: Authored) {
        val activity = itemView.context as AppCompatActivity

        val fragments = activity.supportFragmentManager.fragments.reversed()
        val clPredicate = { it: Fragment -> it is CommentListFragment }
        val commentList = fragments.find(clPredicate) as CommentListFragment? ?: return

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
}