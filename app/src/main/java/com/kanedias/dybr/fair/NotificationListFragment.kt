package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.dto.*
import kotlinx.coroutines.*

/**
 * Fragment which displays list of notifications for current profile.
 *
 * @author Kanedias
 *
 * Created on 14.10.2018
 */
open class NotificationListFragment: UserContentListFragment() {

    @BindView(R.id.notif_ribbon)
    lateinit var notifRibbon: RecyclerView

    @BindView(R.id.notif_list_area)
    lateinit var ribbonRefresher: SwipeRefreshLayout

    override fun getRibbonView() = notifRibbon
    override fun getRefresher() = ribbonRefresher
    override fun getRibbonAdapter() = notifAdapter
    override fun retrieveData(pageNum: Int) = { Network.loadNotifications(pageNum = pageNum) }

    private val notifAdapter = NotificationListAdapter()

    lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        val view = inflater.inflate(R.layout.fragment_notification_list, container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI()
        setupTheming(view)
        loadMore()

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notifications_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_notifications_mark_all_read -> markAllRead()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun markAllRead() {
        val markRoutine = {
            GlobalScope.launch(Dispatchers.Main) {
                try {
                    withContext(Dispatchers.IO) { Network.markAllNotificationsRead() }
                    loadMore(true)
                } catch (ex: Exception) {
                    Network.reportErrors(activity, ex)
                }
            }
        }

        MaterialDialog(activity)
                .title(R.string.confirm_action)
                .message(R.string.mark_all_notifications_read)
                .negativeButton(android.R.string.cancel)
                .positiveButton(android.R.string.yes, click = { markRoutine() })
                .show()
    }

    open fun setupUI() {
        ribbonRefresher.setOnRefreshListener { loadMore(true) }
        notifRibbon.layoutManager = LinearLayoutManager(activity)
        notifRibbon.adapter = notifAdapter
    }

    open fun setupTheming(view: View) {
    }

    /**
     * Loads the next page in notifications listing. If no pages were loaded before, loads first
     * @param reset reset page counter to first
     */
    override fun loadMore(reset: Boolean) {
        if (Auth.profile == null) { // we don't have a profile, just show empty list
            ribbonRefresher.isRefreshing = false
            return
        }

        super.loadMore(reset)
    }

    /**
     * Main adapter of this fragment's recycler view. Shows notifications and handles
     * refreshing and page loading.
     */
    inner class NotificationListAdapter : UserContentListFragment.LoadMoreAdapter() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                ITEM_REGULAR -> {
                    val entryHolder = holder as NotificationViewHolder
                    entryHolder.setup(items[position] as Notification)
                }
                else -> super.onBindViewHolder(holder, position)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_notification_list_item, parent, false)
                    NotificationViewHolder(view)
                }
                else -> super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}