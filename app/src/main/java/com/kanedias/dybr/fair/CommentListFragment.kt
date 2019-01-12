package com.kanedias.dybr.fair

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.Comment
import com.kanedias.dybr.fair.dto.Entry
import com.kanedias.dybr.fair.themes.*
import kotlinx.coroutines.*

/**
 * Fragment which displays selected entry and its comments below.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CommentListFragment : UserContentListFragment() {

    @BindView(R.id.comments_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.comments_list_area)
    lateinit var ribbonRefresher: SwipeRefreshLayout

    @BindView(R.id.comments_ribbon)
    lateinit var commentRibbon: RecyclerView

    @BindView(R.id.add_comment_button)
    lateinit var addCommentButton: FloatingActionButton

    override fun getRibbonView() = commentRibbon
    override fun getRefresher() = ribbonRefresher
    override fun getRibbonAdapter() = commentAdapter
    override fun retrieveData(pageNum: Int) = { Network.loadComments(entry = this.entry!!, pageNum = pageNum) }

    var entry: Entry? = null

    private lateinit var commentAdapter: UserContentListFragment.LoadMoreAdapter

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.get("entry")?.let { entry = it as Entry }

        val view = inflater.inflate(R.layout.fragment_comment_list, container, false)
        activity = context as MainActivity
        commentAdapter = CommentListAdapter()

        ButterKnife.bind(this, view)
        setupUI(view)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("entry", entry)
    }

    private fun setupUI(view: View) {
        toolbar.title = entry?.title
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        ribbonRefresher.setOnRefreshListener { loadMore(reset = true) }
        commentRibbon.layoutManager = LinearLayoutManager(activity)
        commentRibbon.adapter = commentAdapter

        setBlogTheme(view)
    }

    private fun setBlogTheme(view: View) {
        // this is a fullscreen fragment, add new style
        Scoop.getInstance().addStyleLevel(view)
        Scoop.getInstance().bind(TOOLBAR, toolbar)
        Scoop.getInstance().bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        Scoop.getInstance().bind(TOOLBAR_TEXT, toolbar, ToolbarIconAdapter())
        Scoop.getInstance().bind(ACCENT, addCommentButton, FabColorAdapter())
        Scoop.getInstance().bind(TEXT, addCommentButton, FabIconAdapter())
        Scoop.getInstance().bind(BACKGROUND, commentRibbon)
        Scoop.getInstance().bindStatusBar(activity, STATUS_BAR)

        entry?.profile?.get(entry?.document)?.let { applyTheme(it, activity) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Scoop.getInstance().popStyleLevel(false)
    }

    /**
     * Refresh comments displayed in fragment. Entry is not touched but as recycler view is refreshed
     * its views are reloaded too.
     *
     * @param reset if true, reset page counting and start from page one
     */
    override fun loadMore(reset: Boolean) {
        if (entry == null) { // we don't have an entry, just show empty list
            ribbonRefresher.isRefreshing = false
            return
        }

        if (reset) {
            getRibbonView().scrollTo(0, 0)
            commentAdapter.clearItems()
            allLoaded = false
            currentPage = 1
        }

        GlobalScope.launch(Dispatchers.Main) {
            ribbonRefresher.isRefreshing = true

            try {
                val entryDemand = withContext(Dispatchers.IO) { Network.loadEntry(entry!!.id) }
                val commentsDemand = withContext(Dispatchers.IO) { Network.loadComments(entry!!, pageNum = currentPage) }
                entry!!.apply { meta = entryDemand.meta } // refresh comment num and participants
                onMoreDataLoaded(commentsDemand)

                // mark related notifications read
                val markedRead = withContext(Dispatchers.IO) { Network.markNotificationsReadFor(entry!!) }
                if (markedRead) {
                    // we changed notifications, update fragment with them if present
                    val notifPredicate = { it: Fragment -> it is NotificationListFragment }
                    val notifFragment = fragmentManager?.fragments?.find(notifPredicate) as NotificationListFragment?
                    notifFragment?.loadMore(true)
                }
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            ribbonRefresher.isRefreshing = false
        }
    }

    @OnClick(R.id.add_comment_button)
    fun addComment() {
        val commentAdd = CreateNewCommentFragment().apply {
            this.entry = this@CommentListFragment.entry!! // at this point we know we have the entry
        }

        fragmentManager!!.beginTransaction()
                .addToBackStack("Showing comment add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, commentAdd)
                .commit()
    }

    /**
     * Adapter for comments list. Top item is an entry being viewed.
     * All views below represent comments to this entry.
     *
     * Scrolling to the bottom calls polling of next page or stops in case it's the last one
     */
    inner class CommentListAdapter : UserContentListFragment.LoadMoreAdapter() {

        init {
            headers.add(entry!!)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                ITEM_HEADER -> {
                    val entry = headers[position] as Entry
                    (holder as EntryViewHolder).apply {
                        setup(entry, false)
                        itemView.isClickable = false
                    }
                }
                ITEM_REGULAR -> {
                    val comment = items[position - headers.size] as Comment
                    (holder as CommentViewHolder).setup(comment)
                }
                else -> super.onBindViewHolder(holder, position)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                ITEM_HEADER -> {
                    val view = inflater.inflate(R.layout.fragment_entry_list_item, parent, false)
                    EntryViewHolder(view, parent as View, allowSelection = true)
                }
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_comment_list_item, parent, false)
                    CommentViewHolder(entry!!, view, parent as View)
                }
                else -> return super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}