package com.kanedias.dybr.fair.scheduling

import android.app.PendingIntent
import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.html2md.Html2Markdown
import java.util.concurrent.TimeUnit
import android.content.Intent
import com.kanedias.dybr.fair.dto.Notification
import ru.noties.markwon.Markwon
import java.lang.Exception
import java.util.*

/**
 * Sync job that periodically retrieves notifications and notifies user about non-read ones in status bar.
 *
 * @see SyncJobCreator
 * @see InternalReceiver
 * @author Kanedias
 *
 * Created on 21.10.18
 */
class SyncNotificationsJob: Job() {

    override fun onRunJob(params: Params): Result {
        if (MainActivity.onScreen) {
            // we don't want notifications to be showing when we have activity open
            return Result.RESCHEDULE
        }

        // from now on please keep in mind that website notifications != android notifications
        // website notifications represent what comment was left where and by whom
        // and Android notifications are used to make a status update so it will be visible on the screen

        // retrieve website notifications
        val notifications = try {
            Network.loadNotifications()
        } catch (ex: Exception) {
            Network.reportErrors(context, ex)
            emptyList<Notification>()
        }

        // filter them so we only process non-read and non-skipped ones
        val nonRead = notifications.filter { it.state == "new" }
        val nonSkipped = nonRead.filter { !skippedNotificationIds.contains(it.id) }

        if (nonSkipped.isEmpty()) {
            // cancel all android notifications if there's nothing to display
            NotificationManagerCompat.from(context).cancel(NEW_COMMENTS_NOTIFICATION)
            return Result.SUCCESS
        }

        // create grouping android notification
        val groupStyle = NotificationCompat.InboxStyle()
        nonSkipped.forEach { msgNotif ->
            val author = msgNotif.profile.get(msgNotif.document) ?: return@forEach
            val source = msgNotif.source.get(msgNotif.document)
            groupStyle.addLine("${author.nickname} · ${source.blogTitle}")
        }

        // what to do on android notification click
        // currently simple and same for all notifications - just opens notifications tab in main activity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NOTIF_OPEN
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openPI = PendingIntent.getActivity(context, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val groupNotif = NotificationCompat.Builder(context, NC_SYNC_NOTIFICATIONS)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(context.getString(R.string.new_comments))
                .setContentText(context.getString(R.string.youve_received_new_comments))
                .setGroup(context.getString(R.string.notifications))
                .setGroupSummary(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setStyle(groupStyle)
                .setContentIntent(openPI)
                .build()

        NotificationManagerCompat.from(context).notify(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION, groupNotif)

        // convert each website notification to android notification
        for (msgNotif in nonSkipped) {
            val me = Auth.profile?.nickname ?: return Result.SUCCESS // shouldn't happen
            val comment = msgNotif.comment.get(msgNotif.document) ?: continue // comment itself
            val author = msgNotif.profile.get(msgNotif.document) ?: continue // profile of person who wrote the comment
            val source = msgNotif.source.get(msgNotif.document)

            // get data from website notification
            val text = Html2Markdown().parse(comment.content).lines().first() + "..."
            val converted = Markwon.markdown(context, text).toString()
            val msgStyle = NotificationCompat.MessagingStyle(me)
                    .setConversationTitle(source.blogTitle)
                    .addMessage(converted, comment.createdAt.time, author.nickname)

            // fill Android notification intents
            // what to do on action click
            val markReadIntent = Intent(context, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_MARK_READ
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(EXTRA_NOTIF_ID, msgNotif.id)
            }
            val mrPI = PendingIntent.getBroadcast(context, UUID.randomUUID().hashCode(), markReadIntent, 0)

            // what to do on notification swipe
            val skipIntent = Intent(context, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_SKIP
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(EXTRA_NOTIF_ID, msgNotif.id)
            }
            val skipPI = PendingIntent.getBroadcast(context, UUID.randomUUID().hashCode(), skipIntent, 0)

            // android notification itself
            val statusUpdate = NotificationCompat.Builder(context, NC_SYNC_NOTIFICATIONS)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(context.getString(R.string.new_comments))
                    .setContentText(context.getString(R.string.youve_received_new_comments))
                    .setGroup(context.getString(R.string.notifications))
                    .setOnlyAlertOnce(true)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setStyle(msgStyle)
                    .setDeleteIntent(skipPI)
                    .setContentIntent(openPI)
                    .addAction(R.drawable.done, context.getString(R.string.mark_read), mrPI)
                    .build()

            // make Android OS show the created notification
            NotificationManagerCompat.from(context).notify(msgNotif.id, NEW_COMMENTS_NOTIFICATION, statusUpdate)
        }

        // track what's shown currently in status bar
        // Keep in mind that this is a set so duplicates will be skipped
        currentlyShownIds.addAll(nonSkipped.map { it -> it.id })

        return Result.SUCCESS
    }

    companion object {
        /**
         * This set always represents what website notifications ids are currently shown in status bar.
         */
        val currentlyShownIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

        /**
         * This set keeps track of skipped website notification ids. These notifications will never be
         * again shown to the user.
         */
        val skippedNotificationIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())

        /**
         * Called on application/activity start. Schedules periodic job executions
         */
        fun scheduleJob(): Int {
            JobRequest.Builder(JOB_TAG_NOTIFICATIONS_INITIAL)
                    .setExecutionWindow(5_000L, 30_000L)
                    .setBackoffCriteria(5_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequirementsEnforced(true)
                    .build()
                    .schedule()

            return JobRequest.Builder(JOB_TAG_NOTIFICATIONS_PERIODIC)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }

        /**
         * Mark android notification as read and remove it from the status bar
         * @param notifId website notification id associated with this android notification
         * @param ctx context to use when injecting [NotificationManagerCompat]
         */
        fun markRead(ctx: Context, notifId: String) {
            val nm = NotificationManagerCompat.from(ctx)

            nm.cancel(notifId, NEW_COMMENTS_NOTIFICATION)
            currentlyShownIds.remove(notifId)

            // hide summary android notification if there's nothing to group
            if (currentlyShownIds.isEmpty()) {
                nm.cancel(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION)
            }
        }

        /**
         * Mark android notification as skipped. User himself removed this notification from the status bar,
         * we don't need to do anything manually.
         *
         * @param notifId website notification id associated with this android notification
         * @param ctx context to use when injecting [NotificationManagerCompat]
         */
        fun markSkipped(ctx: Context, notifId: String) {
            val nm = NotificationManagerCompat.from(ctx)

            skippedNotificationIds.add(notifId)
            currentlyShownIds.remove(notifId)

            // hide summary android notification if there's nothing to group
            if (currentlyShownIds.isEmpty()) {
                nm.cancel(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION)
            }
        }

    }

}