package com.kanedias.dybr.fair

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.ftinc.scoop.Scoop
import com.google.android.material.card.MaterialCardView
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.misc.getTopFragment
import com.kanedias.dybr.fair.misc.resolveAttr
import com.kanedias.dybr.fair.scheduling.SyncNotificationsWorker
import kotlinx.coroutines.*
import moe.banana.jsonapi2.ArrayDocument
import moe.banana.jsonapi2.Resource

/**
 * Fragment which displays selected entry and its comments below.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CommentListFragment : UserContentListFragment() {

    companion object {
        const val ENTRY_ARG = "entry"
        const val COMMENT_ID_ARG = "comment"
    }

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
        Network.loadComments(entry = this.entry, pageNum = pageNum)
    }

    lateinit var entry: Entry

    private lateinit var commentAdapter: LoadMoreAdapter

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        entry = requireArguments().get(ENTRY_ARG) as Entry

        val view = inflater.inflate(R.layout.fragment_comment_list, container, false)
        activity = context as MainActivity
        commentAdapter = CommentListAdapter()

        ButterKnife.bind(this, view)
        setupUI()
        setupTheming()
        loadMore()

        return view
    }

    private fun setupUI() {
        toolbar.title = entry.title
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }

        ribbonRefresher.setOnRefreshListener { loadMore(reset = true) }
        commentRibbon.adapter = commentAdapter

        if (!isEntryWritable(entry))
            addCommentButton.visibility = View.GONE
    }

    private fun setupTheming() {
        // this is a fullscreen fragment, add new style
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(ACCENT, addCommentButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, addCommentButton, FabIconAdapter())

        styleLevel.bind(BACKGROUND, commentRibbon)
        styleLevel.bindStatusBar(activity, STATUS_BAR)

        val backgrounds = mapOf<View, Int>(commentRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)

        (entry.community ?: entry.profile).get(entry.document)?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }

    /**
     * Refresh comments displayed in fragment. Entry is not touched but as recycler view is refreshed
     * its views are reloaded too.
     *
     * @param reset if true, reset page counting and start from page one
     */
    override fun loadMore(reset: Boolean) {
        // update entry comment meta counters and mark notifications as read for current entry
        lifecycleScope.launch {
            try {
                entry = withContext(Dispatchers.IO) { Network.loadEntry(entry.id) }
                getRibbonAdapter().replaceHeader(0, entry)

                // mark related notifications read
                if (Auth.profile == null)
                    return@launch // nothing to mark, we're nobody

                val markedRead = withContext(Dispatchers.IO) { Network.markNotificationsReadFor(entry) }
                if (markedRead) {
                    // we changed notifications, update fragment with them if present
                    val notifFragment = activity.getTopFragment(NotificationListFragment::class)
                    notifFragment?.loadMore(reset = true)
                }

                // update android notifications
                SyncNotificationsWorker.markReadFor(activity, entry.id)
            } catch (ex: Exception) {
                Network.reportErrors(context, ex)
            }
        }

        // load the actual comments
        if (!requireArguments().containsKey(COMMENT_ID_ARG)) {
            super.loadMore(reset)
            return
        }

        // we're first-loading till comment is visible
        val hlCommentId = requireArguments().getString(COMMENT_ID_ARG)
        lifecycleScope.launchWhenResumed {
            try {
                getRefresher().isRefreshing = true
                var page = 0

                while (true) {
                    val data = withContext(Dispatchers.IO) {
                        retrieveData(pageNum = ++page, starter = pageStarter).invoke()
                    }
                    onMoreDataLoaded(data)

                    val commentIdx = getRibbonAdapter().items.indexOfFirst { it.id == hlCommentId }
                    if (commentIdx != -1) {
                        val commentPos = commentIdx + getRibbonAdapter().headers.size
                        highlightItem(commentPos)
                        break
                    }

                    // should be lower than comment index search, or last comments won't be found
                    if (allLoaded) {
                        break
                    }
                }

            } catch (ex: Exception) {
                Network.reportErrors(context, ex)
            } finally {
                // we don't need to highlight the comment every time
                requireArguments().remove(COMMENT_ID_ARG)
            }

            getRefresher().isRefreshing = false
        }
    }

    private fun highlightItem(position: Int) {
        // the problem with highlighting is that in our recycler views all messages are of different height
        // when recycler view is asked to scroll to some position, it doesn't know their height in advance
        // so we have to scroll continually till all the messages have been laid out and parsed
        lifecycleScope.launchWhenResumed {
            delay(300)
            getRibbonView().scrollToPosition(position)

            var limit = 20 // 2 sec
            while(getRibbonView().findViewHolderForAdapterPosition(position) == null) {
                // holder view hasn't been laid out yet
                delay(100)
                limit -= 1


                if (limit == 0) {
                    // strange, we waited for message for too long to be viewable
                    Log.e("[TopicContent]", "Couldn't find holder for comment $position, this shouldn't happen!")
                    return@launchWhenResumed
                }
            }

            // highlight message by tinting background
            val holder = getRibbonView().findViewHolderForAdapterPosition(position) ?: return@launchWhenResumed
            val card = holder.itemView as CardView
            ValueAnimator.ofArgb(Color.RED, card.cardBackgroundColor.defaultColor).apply {
                addUpdateListener {
                    card.setCardBackgroundColor(it.animatedValue as Int)
                }
                duration = 1500
                start()
            }
        }
    }

    @OnClick(R.id.add_comment_button)
    fun addComment() {
        val commentAdd = CreateNewCommentFragment().apply {
            this.entry = this@CommentListFragment.entry // at this point we know we have the entry
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
            headers.add(entry)
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
                    EntryViewHolder(view, this@CommentListFragment, allowSelection = true)
                }
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_comment_list_item, parent, false)
                    CommentViewHolder(view, this@CommentListFragment)
                }
                else -> return super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}