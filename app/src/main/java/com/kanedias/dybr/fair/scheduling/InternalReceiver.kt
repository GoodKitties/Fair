package com.kanedias.dybr.fair.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kanedias.dybr.fair.*
import com.kanedias.dybr.fair.dto.Notification
import com.kanedias.dybr.fair.dto.NotificationRequest
import kotlinx.coroutines.*
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
                val notif = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? Notification ?: return
                val marked = NotificationRequest().apply {
                    id = notif.id
                    state = "read"
                }

                // mark notification as read and get rid of status bar item
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        withContext(Dispatchers.IO) { Network.updateNotification(marked) }
                        SyncNotificationsWorker.markRead(context, notif)
                    } catch (ex: Exception) {
                        Network.reportErrors(context, ex)
                    }
                }
            }
            ACTION_NOTIF_SKIP -> {
                val notif = intent.getSerializableExtra(EXTRA_NOTIFICATION) as? Notification ?: return
                SyncNotificationsWorker.markSkipped(context, notif)
            }
            else -> return
        }
    }
}