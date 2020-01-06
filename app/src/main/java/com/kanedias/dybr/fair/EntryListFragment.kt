package com.kanedias.dybr.fair

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.ftinc.scoop.Scoop
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.misc.setMaxFlingVelocity
import com.kanedias.dybr.fair.misc.showFullscreenFragment
import com.kanedias.dybr.fair.themes.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.banana.jsonapi2.ArrayDocument


/**
 * Fragment which displays list of entries in currently viewed blog.
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
open class EntryListFragment: UserContentListFragment() {

    @BindView(R.id.entry_ribbon)
    lateinit var entryRibbon: RecyclerView

    @BindView(R.id.fast_jump_button)
    lateinit var fastJumpButton: FloatingActionButton

    @BindView(R.id.entry_list_area)
    lateinit var ribbonRefresher: SwipeRefreshLayout

    override fun getRibbonView() = entryRibbon
    override fun getRefresher() = ribbonRefresher
    override fun getRibbonAdapter() = entryAdapter
    override fun retrieveData(pageNum: Int, starter: Long): () -> ArrayDocument<Entry> = {
        Network.loadEntries(prof = this.profile, pageNum = pageNum, starter = starter)
    }

    var profile: OwnProfile? = null

    private lateinit var entryAdapter: LoadMoreAdapter
    protected lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (profile == null) {
            // restore only if we are recreating old fragment
            savedInstanceState?.get("profile")?.let { profile = it as OwnProfile }
        }

        val view = inflater.inflate(layoutToUse(), container, false)
        activity = context as MainActivity
        entryAdapter = EntryListAdapter()

        ButterKnife.bind(this, view)
        setupUI()
        setupTheming()
        loadMore()

        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("profile", profile)
    }

    open fun layoutToUse() = R.layout.fragment_entry_list

    open fun setupUI() {
        ribbonRefresher.setOnRefreshListener { loadMore(reset = true) }
        entryRibbon.onFlingListener = FastJumpListener()
        entryRibbon.layoutManager = LinearLayoutManager(activity)
        entryRibbon.setMaxFlingVelocity(100_000)
        entryRibbon.adapter = entryAdapter

        // show/hide action button when user switches tab to this fragment
        lifecycle.addObserver(object: LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            fun showActionButton() {
                when (isBlogWritable(profile)) {
                    true -> activity.actionButton.show()
                    false -> activity.actionButton.hide()
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            fun hideActionButton() {
                activity.actionButton.hide()
            }
        })
    }

    open fun setupTheming() {
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(BACKGROUND, entryRibbon)

        styleLevel.bind(ACCENT, fastJumpButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, fastJumpButton, FabIconAdapter())

        val backgrounds = mapOf<View, Int>(entryRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)
        Auth.profile?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }

    /**
     * Entry fragment is mostly bound to profile, so in case we don't have it we should skip loading
     */
    override fun handleLoadSkip(): Boolean {
        if (profile == null) { // we don't have a blog, just show empty list
            ribbonRefresher.isRefreshing = false
            return true
        }

        if (profile?.blogSlug == null) {
            // profile doesn't have a blog yet, ask to create
            entryRibbon.adapter = EmptyBlogAdapter()
            allLoaded = true
            ribbonRefresher.isRefreshing = false
            return true
        }

        return false
    }

    /**
     * Loads the next page in entry listing. If no pages were loaded before, loads first
     * @param reset if true, reset page counting and start from page one
     */
    override fun loadMore(reset: Boolean) {
        super.loadMore(reset)

        if (isBlogWritable(profile)) {
            activity.actionButton.show()
        }
    }

    /**
     * Create new entry in current blog. Shows fragment over the main content allowing to enter the
     * title and the text with an editor.
     */
    fun addCreateNewEntryForm() {
        val entryAdd = CreateNewEntryFragment().apply {
            this.profile = this@EntryListFragment.profile!! // at this point we know we have the blog
        }

        requireActivity().showFullscreenFragment(entryAdd)
    }

    /**
     * Adapter for showing "create blog" button if selected profile doesn't yet have a blog
     */
    inner class EmptyBlogAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.fragment_entry_list_no_blog_item, parent, false)
            return object: RecyclerView.ViewHolder(view) {}
        }

        override fun getItemCount() = 1

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            holder.itemView.setOnClickListener { activity.createBlog() }
        }

    }

    /**
     * Main adapter of this fragment's recycler view. Shows posts in the blog and handles
     * refreshing and page loading.
     */
    inner class EntryListAdapter : UserContentListFragment.LoadMoreAdapter() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                ITEM_REGULAR -> {
                    val entryHolder = holder as EntryViewHolder
                    val entry = items[position] as Entry
                    entryHolder.setup(entry)
                }
                else -> super.onBindViewHolder(holder, position)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_entry_list_item, parent, false)
                    EntryViewHolder(view, this@EntryListFragment)
                }
                else -> super.onCreateViewHolder(parent, viewType)
            }
        }
    }

    /**
     * Show fling up/down buttons on fast scroll of recycler view
     */
    inner class FastJumpListener : RecyclerView.OnFlingListener() {

        private val minTriggerSpeed = 10000

        private var fadeJob: Job? = null

        override fun onFling(velocityX: Int, velocityY: Int): Boolean {
            if (fastJumpButton.visibility == View.VISIBLE) {
                return false
            }

            when {
                velocityY > minTriggerSpeed -> {
                    allowFastJump()
                    fastJumpButton.setOnClickListener { entryRibbon.fling(0, 100_000) }
                    fastJumpButton.rotation = -90f
                }
                velocityY < -minTriggerSpeed -> {
                    allowFastJump()
                    fastJumpButton.setOnClickListener { entryRibbon.fling(0, -100_000) }
                    fastJumpButton.rotation = 90f
                }
            }
            return false
        }

        private fun allowFastJump() {
            // cancel fading if it's in progress
            fadeJob?.cancel()

            fastJumpButton.visibility = View.VISIBLE
            fastJumpButton.alpha = 0.5f

            fadeJob = lifecycleScope.launch {
                delay(2000)

                fastJumpButton.animate()
                        .alpha(0.0f)
                        .setDuration(300)
                        .setListener(object: AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator?) {
                                fastJumpButton.visibility = View.INVISIBLE
                            }
                        })
                        .start()
            }
        }
    }

}