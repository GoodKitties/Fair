package com.kanedias.dybr.fair.scheduling

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import com.evernote.android.job.JobManager
import com.kanedias.dybr.fair.JOB_TAG_NOTIFICATIONS_INITIAL
import com.kanedias.dybr.fair.JOB_TAG_NOTIFICATIONS_PERIODIC

/**
 * Job creator. Responsible for creating scheduled jobs.
 * Required by [JobManager].
 *
 * @author Kanedias
 *
 * Created on 21.10.18
 */
class SyncJobCreator: JobCreator {

    override fun create(tag: String): Job? {
        return when(tag) {
            JOB_TAG_NOTIFICATIONS_PERIODIC, JOB_TAG_NOTIFICATIONS_INITIAL -> SyncNotificationsJob()
            else -> null
        }
    }
}