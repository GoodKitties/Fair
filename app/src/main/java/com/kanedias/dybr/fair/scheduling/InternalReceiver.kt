package com.kanedias.dybr.fair.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationManagerCompat
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.dto.NotificationRequest
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.lang.Exception

/**
 * Broadcast receiver for handling internal intents within the application
 *
 * @author Kanedias
 *
 * Created on 21.10.18
 */
class InternalReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when(intent.action) {
            ACTION_NOTIF_MARK_READ -> {
                val notifId = intent.getStringExtra(EXTRA_NOTIF_ID) ?: return
                val marked = NotificationRequest().apply {
                    id = notifId
                    state = "read"
                }

                // mark notification as read and get rid of status bar item
                launch(UI) {
                    try {
                        async { Network.updateNotification(marked) }.await()
                        SyncNotificationsJob.markRead(context, notifId)
                    } catch (ex: Exception) {
                        Network.reportErrors(context, ex)
                    }
                }
            }
            ACTION_NOTIF_SKIP -> {
                val notifId = intent.getStringExtra(EXTRA_NOTIF_ID) ?: return
                SyncNotificationsJob.markSkipped(context, notifId)
            }
            else -> return
        }
    }
}