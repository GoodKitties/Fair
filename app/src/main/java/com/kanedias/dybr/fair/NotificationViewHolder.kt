package com.kanedias.dybr.fair

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import java.text.SimpleDateFormat
import java.util.*
import com.kanedias.dybr.fair.dto.Notification
import com.kanedias.dybr.fair.dto.NotificationRequest
import com.kanedias.dybr.fair.markdown.handleMarkdown
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.scheduling.SyncNotificationsWorker
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.*
import kotlinx.coroutines.*

/**
 * View holder for showing notifications in main tab.
 * @param iv inflated view to be used by this holder
 * @see NotificationListFragment.notifRibbon
 * @author Kanedias
 */
class NotificationViewHolder(iv: View, val parentFragment: UserContentListFragment) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.notification_content_area)
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
    private val commentShow  = View.OnClickListener {
        val activity = it.context as AppCompatActivity

        parentFragment.lifecycleScope.launch {
            try {
                val linkedEntry = withContext(Dispatchers.IO) { Network.loadEntry(notification.entryId) }
                val commentsPage = CommentListFragment().apply { entry = linkedEntry }
                activity.showFullscreenFragment(commentsPage)

                if (notification.state == "new") {
                    switchRead()
                }
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }

    }

    init {
        ButterKnife.bind(this, iv)
        setupTheming()

        iv.setOnClickListener(commentShow)
    }

    @OnClick(R.id.notification_read)
    fun switchRead() {
        val marked = NotificationRequest().apply {
            id = notification.id
            state = if (notification.state == "new") { "read" } else { "new" }
        }

        parentFragment.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { Network.updateNotification(marked) }
                SyncNotificationsWorker.markRead(itemView.context, notification)

                notification.state = marked.state
                updateState()
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }
        }
    }

    private fun setupTheming() {
        val styleLevel = parentFragment.styleLevel

        styleLevel.bind(TEXT_BLOCK, itemView, CardViewColorAdapter())
        styleLevel.bind(TEXT, causeView, TextViewDisableAwareColorAdapter())
        styleLevel.bind(TEXT, blogView, TextViewDisableAwareColorAdapter())
        styleLevel.bind(TEXT, authorView, TextViewDisableAwareColorAdapter())
        styleLevel.bind(TEXT, dateView, TextViewDisableAwareColorAdapter())
        styleLevel.bind(TEXT, bodyView, TextViewDisableAwareColorAdapter())
        styleLevel.bind(TEXT_LINKS, bodyView, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, readButton)
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    fun setup(notification: Notification) {
        this.notification = notification

        updateState()

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

    private fun updateState() {
        if (notification.state == "read") {
            readButton.setImageResource(R.drawable.done_all)
            toggleEnableRecursive(notificationArea, enabled = false)
        } else { // state == "new"
            readButton.setImageResource(R.drawable.done)
            toggleEnableRecursive(notificationArea, enabled = true)
        }
    }
}