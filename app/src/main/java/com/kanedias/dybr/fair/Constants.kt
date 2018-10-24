package com.kanedias.dybr.fair

// Constants relevant for other classes

// LOAD_MORE recycler view mechanics
const val ITEM_REGULAR = 0
const val ITEM_LOAD_MORE = 1
const val ITEM_LAST_PAGE = 2

// Job tags for scheduling
const val JOB_TAG_NOTIFICATIONS_INITIAL = "Notifications-oneshot"
const val JOB_TAG_NOTIFICATIONS_PERIODIC = "Notifications-periodic"

// Notification channels
const val NC_SYNC_NOTIFICATIONS = "sync-notifications"

// Notification IDs in notification manager
const val NEW_COMMENTS_NOTIFICATION = 0

// intent extras
const val EXTRA_NOTIF_ID = "notification-id"

// intent actions
const val ACTION_NOTIF_MARK_READ = "com.kanedias.dybr.fair.action.notif.MARK_READ"
const val ACTION_NOTIF_SKIP = "com.kanedias.dybr.fair.action.notif.SKIP"
const val ACTION_NOTIF_OPEN = "com.kanedias.dybr.fair.action.notif.OPEN"