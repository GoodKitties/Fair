package com.kanedias.dybr.fair.scheduling

import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.html2md.Html2Markdown
import android.content.Intent
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.dto.Notification
import com.kanedias.dybr.fair.dto.OwnProfile
import com.kanedias.dybr.fair.ui.mdRendererFrom
import java.lang.Exception
import java.util.*
import java.util.concurrent.TimeUnit.*

/**
 * Sync job that periodically retrieves notifications and notifies user about non-read ones in status bar.
 *
 * @see InternalReceiver
 * @author Kanedias
 *
 * Created on 21.10.18
 */
class SyncNotificationsWorker(val ctx: Context, params: WorkerParameters): Worker(ctx, params) {

    override fun doWork(): Result {

        // from now on please keep in mind that website notifications != android notifications
        // website notifications represent what comment was left where and by whom
        // and Android notifications are used to make a status update so it will be visible on the screen

        // retrieve website notifications
        val notifications = try {
            Network.loadNotifications()
        } catch (ex: Exception) {
            Network.reportErrors(ctx, ex)
            emptyList<Notification>()
        }

        // filter them so we only process non-read and non-skipped ones
        val nonRead = notifications.filter { it.state == "new" }
        val nonSkipped = nonRead - skipped
        val nonSkippedAndNew = nonSkipped - currentlyShown

        if (nonSkippedAndNew.isEmpty()) {
            // cancel all android notifications if there's nothing to display
            NotificationManagerCompat.from(ctx).cancel(NEW_COMMENTS_NOTIFICATION)
            return Result.success()
        }

        // what to do on group notification click - open notifications tab
        val openTabIntent = Intent(ctx, MainActivity::class.java).apply {
            action = ACTION_NOTIF_OPEN
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val openTabPI = PendingIntent.getActivity(ctx, 0, openTabIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // configure own avatar
        val userProfile = Auth.profile ?: return Result.success() // shouldn't happen
        val userPerson = Person.Builder().setName(userProfile.nickname).setIcon(loadAvatar(userProfile)).build()

        // convert each website notification to android notification
        for (msgNotif in nonSkippedAndNew) {
            val comment = msgNotif.comment.get(msgNotif.document) ?: continue // comment itself
            val author = msgNotif.profile.get(msgNotif.document) ?: continue // comment author profile
            val source = msgNotif.source.get(msgNotif.document) // blog owner profile where comment was written

            val authorPerson = Person.Builder().setName(author.nickname).setIcon(loadAvatar(author)).build()

            // get data from website notification
            val text = Html2Markdown().parse(comment.content).lines().firstOrNull { it.isNotEmpty() }.orEmpty() + "..."
            val converted = mdRendererFrom(ctx).toMarkdown(text).toString()
            val msgStyle = NotificationCompat.MessagingStyle(userPerson)
                    .setConversationTitle(source.blogTitle)
                    .addMessage(converted, comment.createdAt.time, authorPerson)

            // fill Android notification intents

            // what to do on android notification click
            val openIntent = Intent(ctx, MainActivity::class.java).apply {
                action = ACTION_NOTIF_OPEN
                putExtra(EXTRA_NOTIFICATION, msgNotif)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val openPI = PendingIntent.getActivity(ctx, UUID.randomUUID().hashCode(), openIntent, 0)

            // what to do on mark read action click
            val markReadIntent = Intent(ctx, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_MARK_READ
                putExtra(EXTRA_NOTIFICATION, msgNotif)
            }
            val mrPI = PendingIntent.getBroadcast(ctx, UUID.randomUUID().hashCode(), markReadIntent, 0)

            // what to do on notification swipe
            val skipIntent = Intent(ctx, InternalReceiver::class.java).apply {
                action = ACTION_NOTIF_SKIP
                putExtra(EXTRA_NOTIFICATION, msgNotif)
            }
            val skipPI = PendingIntent.getBroadcast(ctx, UUID.randomUUID().hashCode(), skipIntent, 0)

            // android notification itself
            val statusUpdate = NotificationCompat.Builder(ctx, NC_SYNC_NOTIFICATIONS)
                    .setSmallIcon(R.drawable.app_icon)
                    .setContentTitle(ctx.getString(R.string.new_comments))
                    .setContentText(ctx.getString(R.string.youve_received_new_comments))
                    .setGroup(ctx.getString(R.string.notifications))
                    .setOnlyAlertOnce(true)
                    .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setStyle(msgStyle)
                    .setDeleteIntent(skipPI)
                    .setContentIntent(openPI)
                    .addAction(R.drawable.done, ctx.getString(R.string.mark_read), mrPI)
                    .build()

            // make Android OS show the created notification
            NotificationManagerCompat.from(ctx).notify(msgNotif.id, NEW_COMMENTS_NOTIFICATION, statusUpdate)
        }

        // create grouping android notification
        val groupStyle = NotificationCompat.InboxStyle()
        nonSkippedAndNew.forEach { msgNotif ->
            val author = msgNotif.profile.get(msgNotif.document) ?: return@forEach
            val source = msgNotif.source.get(msgNotif.document)
            groupStyle.addLine("${author.nickname} · ${source.blogTitle}")
        }

        val groupNotif = NotificationCompat.Builder(ctx, NC_SYNC_NOTIFICATIONS)
                .setSmallIcon(R.drawable.app_icon)
                .setContentTitle(ctx.getString(R.string.new_comments))
                .setContentText(ctx.getString(R.string.youve_received_new_comments))
                .setGroup(ctx.getString(R.string.notifications))
                .setGroupSummary(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setStyle(groupStyle)
                .setContentIntent(openTabPI)
                .build()

        NotificationManagerCompat.from(ctx).notify(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION, groupNotif)

        // track what's shown currently in status bar
        // Keep in mind that this is a set so duplicates will be skipped
        currentlyShown += nonSkippedAndNew

        return Result.success()
    }

    private fun loadAvatar(prof: OwnProfile): IconCompat? {
        if (prof.settings?.avatar == null)
            return null

        return try {
            val bitmap = Glide.with(ctx).asBitmap()
                    .apply(RequestOptions().circleCrop())
                    .load(prof.settings?.avatar).submit().get(5, SECONDS)
            IconCompat.createWithBitmap(bitmap)
        } catch (ex: Exception) {
            Log.e("SyncJob", "Couldn't load avatar for profile ${prof.nickname}")
            null
        }
    }

    companion object {
        /**
         * This set always represents what website notifications ids are currently shown in status bar.
         */
        val currentlyShown: MutableSet<Notification> = Collections.synchronizedSet(mutableSetOf<Notification>())

        /**
         * This set keeps track of skipped website notification ids. These notifications will never be
         * again shown to the user.
         */
        val skipped: MutableSet<Notification> = Collections.synchronizedSet(mutableSetOf<Notification>())

        /**
         * Called on application/activity start. Schedules periodic job executions
         */
        fun scheduleJob(ctx: Context) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            val periodMinutes = prefs.getString("notification-check-interval", "15")?.toLong() ?: return

            if (periodMinutes <= 0) {
                // user selected not to check for notifications
                return
            }

            // how much to wait before executing the job for the first time
            val flexPeriodSeconds = MINUTES.toSeconds(periodMinutes) - 15

            val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncNotificationsWorker>(periodMinutes, MINUTES, flexPeriodSeconds, SECONDS)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, SECONDS)
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .setRequiresBatteryNotLow(true)
                            .build())
                    .build()

            WorkManager.getInstance().enqueueUniquePeriodicWork(
                    SYNC_NOTIFICATIONS_UNIQUE_JOB,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWorkRequest)
        }

        /**
         * Mark android notification as read and remove it from the status bar
         * @param notif website notification associated with this android notification
         * @param ctx context to use when injecting [NotificationManagerCompat]
         */
        fun markRead(ctx: Context, notif: Notification) {
            val nm = NotificationManagerCompat.from(ctx)

            nm.cancel(notif.id, NEW_COMMENTS_NOTIFICATION)
            currentlyShown.remove(notif)

            // hide summary android notification if there's nothing to group
            if (currentlyShown.isEmpty()) {
                nm.cancel(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION)
            }
        }

        fun markReadFor(ctx: Context, entryId: String) {
            val shownForEntry = currentlyShown.filter { it.entryId == entryId }
            shownForEntry.forEach { markRead(ctx, it) }
        }

        /**
         * Mark android notification as skipped. User himself removed this notification from the status bar,
         * we don't need to do anything manually.
         *
         * @param notif website notification associated with this android notification
         * @param ctx context to use when injecting [NotificationManagerCompat]
         */
        fun markSkipped(ctx: Context, notif: Notification) {
            val nm = NotificationManagerCompat.from(ctx)

            skipped.add(notif)
            currentlyShown.remove(notif)

            // hide summary android notification if there's nothing to group
            // not actually required, android deletes group one along with the last
            if (currentlyShown.isEmpty()) {
                nm.cancel(NEW_COMMENTS_NOTIFICATION_SUMMARY_TAG, NEW_COMMENTS_NOTIFICATION)
            }
        }

    }

}