package com.kanedias.dybr.fair

import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import java.text.SimpleDateFormat
import java.util.*
import com.kanedias.dybr.fair.dto.Notification
import com.kanedias.dybr.fair.dto.NotificationRequest
import com.kanedias.dybr.fair.ui.toggleEnableRecursive
import kotlinx.coroutines.*

/**
 * View holder for showing notifications in main tab.
 * @param iv inflated view to be used by this holder
 * @see NotificationListFragment.notifRibbon
 * @author Kanedias
 */
class NotificationViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.notification_area)
    lateinit var notificationArea: RelativeLayout

    @BindView(R.id.notification_cause)
    lateinit var causeView: TextView

    @BindView(R.id.notification_profile)
    lateinit var authorView: TextView

    @BindView(R.id.notification_date)
    lateinit var dateView: TextView

    @BindView(R.id.notification_blog)
    lateinit var blogView: TextView

    @BindView(R.id.notification_message)
    lateinit var bodyView: TextView

    @BindView(R.id.notification_divider)
    lateinit var divider: View

    @BindView(R.id.notification_read)
    lateinit var readButton: ImageView

    /**
     * Entry that this holder represents
     */
    private lateinit var notification: Notification

    /**
     * Listener to show comments of this entry
     */
    private val commentShow  = View.OnClickListener { it ->
        val activity = it.context as AppCompatActivity

        GlobalScope.launch(Dispatchers.Main) {
            try {
                val linkedEntry = async(Dispatchers.IO) { Network.loadEntry(notification.entryId) }.await()
                val commentsPage = CommentListFragment().apply { entry = linkedEntry }
                activity.supportFragmentManager.beginTransaction()
                        .addToBackStack("Showing comment list fragment from notification")
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .add(R.id.main_drawer_layout, commentsPage)
                        .commit()
                markRead()
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }

    }

    init {
        ButterKnife.bind(this, iv)

        iv.setOnClickListener(commentShow)
    }

    @OnClick(R.id.notification_read)
    fun markRead() {
        val marked = NotificationRequest().apply {
            id = notification.id
            state = "read"
        }

        GlobalScope.launch(Dispatchers.Main) {
            try {
                async(Dispatchers.IO) { Network.updateNotification(marked) }.await()
                readButton.setImageResource(R.drawable.done_all)
                toggleEnableRecursive(notificationArea, enabled = false)
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    fun setup(notification: Notification) {
        if (notification.state == "read") {
            readButton.setImageResource(R.drawable.done_all)
            toggleEnableRecursive(notificationArea, enabled = false)
        } else {
            readButton.setImageResource(R.drawable.done)
            toggleEnableRecursive(notificationArea, enabled = true)
        }

        this.notification = notification

        val comment = notification.comment.get(notification.document)
        val profile = notification.profile.get(notification.document)
        val source = notification.source.get(notification.document)

        // setup text views from entry data
        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(comment.createdAt)
        causeView.setText(R.string.comment)
        authorView.text = profile.nickname
        blogView.text = source.blogTitle
        bodyView.handleMarkdown(comment.content)
    }
}