package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import moe.banana.jsonapi2.ArrayDocument


/**
 * Fragment which displays list of notifications for current profile.
 *
 * @author Kanedias
 *
 * Created on 14.10.2018
 */
open class NotificationListFragment: Fragment() {

    @BindView(R.id.notif_ribbon)
    lateinit var notifRibbon: RecyclerView

    @BindView(R.id.notif_list_area)
    lateinit var refresher: SwipeRefreshLayout

    private val notifAdapter = NotificationListAdapter()
    private var nextPage = 1
    private var lastPage = false

    lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_notification_list, container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI(view)
        setupTheming()
        refreshNotifications()
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_notifications_mark_all_read -> markAllRead()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun markAllRead() {
        val markRoutine = {
            launch(UI) {
                try {
                    async { Network.markAllNotificationsRead() }
                    refreshNotifications(true)
                } catch (ex: Exception) {
                    Network.reportErrors(activity, ex)
                }
            }
        }

        MaterialDialog.Builder(activity)
                .title(R.string.confirm_action)
                .content(R.string.mark_all_notifications_read)
                .negativeText(android.R.string.cancel)
                .positiveText(android.R.string.yes)
                .onPositive {_, _ ->  markRoutine() }
                .show()
    }

    open fun setupUI(view: View) {
        refresher.setOnRefreshListener { refreshNotifications(true) }
        notifRibbon.layoutManager = LinearLayoutManager(activity)
        notifRibbon.adapter = notifAdapter
    }

    open fun setupTheming() {
    }

    /**
     * Loads the next page in notifications listing. If no pages were loaded before, loads first
     * @param reset reset page counter to first
     */
    fun refreshNotifications(reset: Boolean = false) {
        if (Auth.profile == null) { // we don't have a profile, just show empty list
            refresher.isRefreshing = false
            return
        }

        if (reset) {
            notifRibbon.smoothScrollToPosition(0)
            nextPage = 1
            lastPage = false
        }

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async { Network.loadNotifications(nextPage) }
                updateRibbonPage(success.await(), reset)
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
        }
    }

    /**
     * Update notification ribbon with newly loaded values.
     * @param loaded document with notifications for active profile and links to pages that was loaded
     * @param reset if true, clear current entries before populating from [loaded]
     */
    private fun updateRibbonPage(loaded: ArrayDocument<Notification>, reset: Boolean) {
        if (reset) {
            notifAdapter.notifications.clear()
        }

        if (loaded.isEmpty()) {
            lastPage = true
            notifAdapter.notifyDataSetChanged()
            return
        }

        nextPage += 1
        notifAdapter.apply {
            notifications.addAll(loaded)
            notifyDataSetChanged()
        }
    }

    /**
     * Main adapter of this fragment's recycler view. Shows notifications and handles
     * refreshing and page loading.
     */
    inner class NotificationListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val REGULAR = 0
        private val LOAD_MORE = 1
        private val LAST_PAGE = 2

        var notifications: MutableList<Notification> = ArrayList()

        override fun getItemViewType(position: Int): Int {
            if (position < notifications.size) {
                return REGULAR
            }

            if (lastPage) {
                return  LAST_PAGE
            }

            return LOAD_MORE
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                REGULAR -> {
                    val entryHolder = holder as NotificationViewHolder
                    val notification = notifications[position]
                    entryHolder.setup(notification)
                }
                LOAD_MORE -> refreshNotifications()
                // Nothing needed for LAST_PAGE
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_notification_list_item, parent, false)
                    NotificationViewHolder(view, this)
                }
                LOAD_MORE -> {
                    val pbar = inflater.inflate(R.layout.view_load_more, parent, false)
                    object: RecyclerView.ViewHolder(pbar) {}
                }
                else -> { // LAST_PAGE
                    val lastPage = inflater.inflate(R.layout.view_last_page, parent, false)
                    lastPage.findViewById<TextView>(R.id.last_page_reload).setOnClickListener { refreshNotifications(true) }
                    object: RecyclerView.ViewHolder(lastPage) {}
                }
            }
        }

        override fun getItemCount() : Int {
            if (notifications.isEmpty()) {
                // seems like we didn't yet load anything in our view, it's probably loading
                // for the first time. Don't show "load more", let's wait while anything shows up.
                return 0
            }

            // we have something in the view already
            return notifications.size + 1
        }

        fun removeItem(position: Int) {
            notifications.removeAt(position)
            notifyItemRemoved(position)
        }
    }

}