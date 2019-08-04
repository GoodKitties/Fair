package com.kanedias.dybr.fair

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.work.WorkManager
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.themes.*
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraDialog
import org.acra.annotation.AcraMailSender
import org.acra.data.StringFormat
import com.kanedias.dybr.fair.scheduling.SyncNotificationsWorker


/**
 * Place to initialize all data prior to launching activities
 *
 * @author Kanedias
 */
@AcraDialog(resIcon = R.mipmap.ic_launcher, resText = R.string.app_crashed, resCommentPrompt = R.string.leave_crash_comment, resTheme = R.style.AppTheme)
@AcraMailSender(mailTo = "kanedias@xaker.ru", resSubject = R.string.app_crash_report, reportFileName = "crash-report.json")
@AcraCore(buildConfigClass = BuildConfig::class, reportFormat = StringFormat.JSON, alsoReportToAndroidFramework = true)
class MainApplication : Application() {

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when(key) {
            "notification-check-interval" -> rescheduleJobs()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        // init crash reporting
        ACRA.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        DbProvider.setHelper(this)
        Network.init(this)
        Auth.init(this)

        initTheming()
        initStatusNotifications()
        rescheduleJobs()

        // load last account if it exists
        val acc = DbProvider.helper.accDao.queryBuilder().where().eq("current", true).queryForFirst()
        acc?.let {
            Auth.updateCurrentUser(acc)
        }

        // setup configuration change listener
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun rescheduleJobs() {
        // don't schedule anything in crash reporter process
        if (ACRA.isACRASenderServiceProcess())
            return

        // stop scheduling current jobs
        WorkManager.getInstance().cancelUniqueWork(SYNC_NOTIFICATIONS_UNIQUE_JOB).result.get()

        // replace with new one
        SyncNotificationsWorker.scheduleJob(this)
    }

    private fun initStatusNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notifMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val syncNotifChannel = NotificationChannel(NC_SYNC_NOTIFICATIONS,
                    getString(R.string.notifications),
                    NotificationManager.IMPORTANCE_HIGH)

            notifMgr.createNotificationChannel(syncNotifChannel)
        }
    }

    private fun initTheming() {
        Scoop.initialize(mapOf(
                BACKGROUND to resources.getColor(R.color.background_material_dark),
                TOOLBAR to resources.getColor(R.color.colorPrimary),
                STATUS_BAR to resources.getColor(R.color.colorPrimaryDark),
                TOOLBAR_TEXT to resources.getColor(R.color.primary_text_default_material_dark),
                TEXT_HEADERS to resources.getColor(R.color.primary_text_default_material_dark),
                TEXT to resources.getColor(R.color.secondary_text_default_material_dark),
                TEXT_LINKS to resources.getColor(android.R.color.white),
                TEXT_BLOCK to resources.getColor(R.color.cardview_dark_background),
                TEXT_OFFTOP to resources.getColor(android.R.color.secondary_text_dark),
                ACCENT to resources.getColor(R.color.colorAccent),
                ACCENT_TEXT to resources.getColor(R.color.primary_text_default_material_dark),
                DIVIDER to resources.getColor(R.color.colorAccent))
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        DbProvider.releaseHelper()
    }
}
