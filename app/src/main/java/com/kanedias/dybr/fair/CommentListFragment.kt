package com.kanedias.dybr.fair

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import com.kanedias.dybr.fair.dto.Auth
import com.kanedias.dybr.fair.dto.Comment
import com.kanedias.dybr.fair.dto.Entry
import com.kanedias.dybr.fair.dto.writable
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.misc.getTopFragment
import com.kanedias.dybr.fair.scheduling.SyncNotificationsWorker
import kotlinx.coroutines.*
import moe.banana.jsonapi2.ArrayDocument

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
    override fun retrieveData(pageNum: Int, starter: Long): () -> ArrayDocument<Comment> = {
        // comments go from new to last, no need for a limiter
        Network.loadComments(entry = this.entry!!, pageNum = pageNum)
    }

    var entry: Entry? = null

    private lateinit var commentAdapter: LoadMoreAdapter

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.get("entry")?.let { entry = it as Entry }

        val view = inflater.inflate(R.layout.fragment_comment_list, container, false)
        activity = context as MainActivity
        commentAdapter = CommentListAdapter()

        ButterKnife.bind(this, view)
        setupUI()
        setupTheming()
        loadMore()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("entry", entry)
    }

    private fun setupUI() {
        toolbar.title = entry?.title
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        ribbonRefresher.setOnRefreshListener { loadMore(reset = true) }
        commentRibbon.layoutManager = LinearLayoutManager(activity)
        commentRibbon.adapter = commentAdapter

        if (!entry.writable) // guests can't post comments
            addCommentButton.visibility = View.GONE
    }

    private fun setupTheming() {
        // this is a fullscreen fragment, add new style
        styleLevel = Scoop.getInstance().addStyleLevel()

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(ACCENT, addCommentButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, addCommentButton, FabIconAdapter())

        styleLevel.bind(BACKGROUND, commentRibbon)
        styleLevel.bindStatusBar(activity, STATUS_BAR)

        val backgrounds = mapOf<View, Int>(commentRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)
        entry?.profile?.get(entry?.document)?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Scoop.getInstance().popStyleLevel(false)
    }

    override fun handleLoadSkip(): Boolean {
        if (entry == null) { // we don't have an entry, just show empty list
            ribbonRefresher.isRefreshing = false
            return true
        }

        return false
    }

    /**
     * Refresh comments displayed in fragment. Entry is not touched but as recycler view is refreshed
     * its views are reloaded too.
     *
     * @param reset if true, reset page counting and start from page one
     */
    override fun loadMore(reset: Boolean) {
        super.loadMore(reset)

        // update entry comment meta counters and mark notifications as read for current entry
        uiScope.launch(Dispatchers.Main) {
            try {
                entry = withContext(Dispatchers.IO) { Network.loadEntry(entry!!.id) }
                getRibbonAdapter().replaceHeader(0, entry!!)

                // mark related notifications read
                if (Auth.profile == null)
                    return@launch // nothing to mark, we're nobody

                val markedRead = withContext(Dispatchers.IO) { Network.markNotificationsReadFor(entry!!) }
                if (markedRead) {
                    // we changed notifications, update fragment with them if present
                    val notifFragment = activity.getTopFragment(NotificationListFragment::class)
                    notifFragment?.loadMore(reset = true)

                    // update android notifications
                    SyncNotificationsWorker.markReadFor(activity, entry!!.id)
                }
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }
        }
    }

    @OnClick(R.id.add_comment_button)
    fun addComment() {
        val commentAdd = CreateNewCommentFragment().apply {
            this.entry = this@CommentListFragment.entry!! // at this point we know we have the entry
        }

        requireActivity().showFullscreenFragment(commentAdd)
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
                        setup(entry)
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
                    EntryViewHolder(view, parent, allowSelection = true)
                }
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_comment_list_item, parent, false)
                    CommentViewHolder(view, parent, entry!!)
                }
                else -> return super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}