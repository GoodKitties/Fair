package com.kanedias.dybr.fair.scheduling

import android.app.PendingIntent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.html2md.Html2Markdown
import java.util.concurrent.TimeUnit
import android.content.Intent

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

        // retrieve non-read non-skipped notifications
        val notifications = Network.loadNotifications()
        val nonRead = notifications.filter { it.state == "new" }
        val nonSkipped = nonRead.filter { !skippedNotificationIds.contains(it.id) }

        if (nonSkipped.isEmpty()) {
            // remove notification if it was shown
            NotificationManagerCompat.from(context).cancel(NEW_COMMENTS_NOTIFICATION)
            return Result.SUCCESS
        }

        // create a notification for each item
        for (notification in nonSkipped) {
            val me = Auth.profile?.nickname ?: return Result.SUCCESS // shouldn't happen
            val comment = notification.comment.get(notification.document) ?: continue
            val profile = notification.profile.get(notification.document) ?: continue
            val text = Html2Markdown().parse(comment.content).lines().first() + "..."
            val msgStyle = NotificationCompat.MessagingStyle(me)
                    .addMessage(text, comment.createdAt.time, profile.nickname)

            val markReadIntent = Intent(context, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_MARK_READ
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(EXTRA_NOTIF_ID, notification.id)
            }
            val mrPI = PendingIntent.getBroadcast(context, 0, markReadIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val skipIntent = Intent(context, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_SKIP
                addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                putExtra(EXTRA_NOTIF_ID, notification.id)
            }
            val skipPI = PendingIntent.getBroadcast(context, 1, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_NOTIF_ID, notification.id)
            }
            val openPI = PendingIntent.getActivity(context, 2, openIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val statusUpdate = NotificationCompat.Builder(context, NC_SYNC_NOTIFICATIONS)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(context.getString(R.string.new_comments))
                    .setContentText(context.getString(R.string.youve_received_new_comments))
                    .setStyle(msgStyle)
                    .setDeleteIntent(skipPI)
                    .setContentIntent(openPI)
                    .addAction(R.drawable.done, context.getString(R.string.mark_read), mrPI)
                    .build()

            NotificationManagerCompat.from(context).notify(notification.id, NEW_COMMENTS_NOTIFICATION, statusUpdate)
        }

        return Result.SUCCESS
    }

    companion object {

        val skippedNotificationIds = mutableSetOf<String>()

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

    }

}