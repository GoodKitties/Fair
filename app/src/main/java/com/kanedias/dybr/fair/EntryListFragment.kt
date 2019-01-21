package com.kanedias.dybr.fair

import android.os.Bundle
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.fragment.app.FragmentTransaction
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.dto.*


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

    @BindView(R.id.entry_list_area)
    lateinit var ribbonRefresher: SwipeRefreshLayout

    override fun getRibbonView() = entryRibbon
    override fun getRefresher() = ribbonRefresher
    override fun getRibbonAdapter() = entryAdapter
    override fun retrieveData(pageNum: Int) = { Network.loadEntries(prof = this.profile, pageNum = pageNum) }

    var profile: OwnProfile? = null

    private lateinit var entryAdapter: UserContentListFragment.LoadMoreAdapter
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
        setupTheming(view)
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
        entryRibbon.layoutManager = LinearLayoutManager(activity)
        entryRibbon.adapter = entryAdapter
    }

    open fun setupTheming(view: View) {
    }

    /**
     * [FragmentStatePagerAdapter] calls this when fragment becomes active (e.g. tab is selected)
     */
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)

        // don't touch any views if fragment is just instantiated and not attached yet
        // FragmentStatePagerAdapter does this when creating items anew
        if (context == null)
            return

        // for now we can only write entries in blog if it's our own
        when (isVisibleToUser && isBlogWritable(profile)) {
            true -> activity.actionButton.show()
            false -> activity.actionButton.hide()
        }
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

        requireFragmentManager().beginTransaction()
                .addToBackStack("Showing entry add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, entryAdd)
                .commit()
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
                    EntryViewHolder(view, parent as View)
                }
                else -> super.onCreateViewHolder(parent, viewType)
            }
        }
    }

}