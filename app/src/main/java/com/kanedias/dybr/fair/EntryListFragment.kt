package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.FragmentTransaction
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.dto.*
import kotlinx.coroutines.*
import moe.banana.jsonapi2.ArrayDocument


/**
 * Fragment which displays list of entries in currently viewed blog.
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
open class EntryListFragment: Fragment() {

    @BindView(R.id.entry_ribbon)
    lateinit var entryRibbon: RecyclerView

    @BindView(R.id.entry_list_area)
    lateinit var refresher: SwipeRefreshLayout

    var profile: OwnProfile? = null

    private val entryAdapter = EntryListAdapter()
    private var nextPage = 1
    private var lastPage = false

    lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (profile == null) {
            // restore only if we are recreating old fragment
            savedInstanceState?.get("profile")?.let { profile = it as OwnProfile }
        }

        val view = inflater.inflate(layoutToUse(), container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI(view)
        setupTheming()
        refreshEntries()
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("profile", profile)
    }

    open fun layoutToUse() = R.layout.fragment_entry_list

    open fun setupUI(view: View) {
        refresher.setOnRefreshListener { refreshEntries(reset = true) }
        entryRibbon.layoutManager = LinearLayoutManager(activity)
        entryRibbon.adapter = entryAdapter
    }

    open fun setupTheming() {
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
     * Loads the next page in entry listing. If no pages were loaded before, loads first
     * @param reset if true, reset page counting and start from page one
     */
    fun refreshEntries(reset: Boolean = false) {
        if (profile == null) { // we don't have a blog, just show empty list
            refresher.isRefreshing = false
            return
        }

        if (profile?.blogSlug == null) {
            // profile doesn't have a blog yet, ask to create
            entryRibbon.adapter = EmptyBlogAdapter()
            return
        }

        if (reset) {
            entryRibbon.smoothScrollToPosition(0)
            nextPage = 1
            lastPage = false
        }

        GlobalScope.launch(Dispatchers.Main) {
            refresher.isRefreshing = true

            try {
                val success = withContext(Dispatchers.IO) { Network.loadEntries(profile!!, nextPage) }
                updateRibbonPage(success, reset)
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false

            if (isBlogWritable(profile)) {
                activity.actionButton.show()
            }
        }
    }

    /**
     * Update entry ribbon with newly loaded values.
     * @param loaded document with entries and links to pages that was loaded
     * @param reset if true, reset page counting and start from page one
     */
    private fun updateRibbonPage(loaded: ArrayDocument<Entry>, reset: Boolean) {
        if (reset) {
            entryAdapter.apply {
                entries.clear()
                notifyDataSetChanged()
            }
        }

        if (loaded.isEmpty()) {
            lastPage = true
            entryAdapter.notifyDataSetChanged()
            return
        }

        nextPage += 1
        entryAdapter.apply {
            val oldSize = entries.size
            entries.addAll(loaded)
            notifyItemRangeInserted(oldSize, loaded.size)
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

        fragmentManager!!.beginTransaction()
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
    inner class EntryListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var entries: MutableList<Entry> = ArrayList()

        override fun getItemViewType(position: Int) = when {
            position < entries.size -> ITEM_REGULAR
            lastPage -> ITEM_LAST_PAGE
            else -> ITEM_LOAD_MORE
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                ITEM_REGULAR -> {
                    val entryHolder = holder as EntryViewHolder
                    val entry = entries[position]
                    entryHolder.setup(entry, isBlogWritable(profile))
                }
                ITEM_LOAD_MORE -> refreshEntries()
                // Nothing needed for ITEM_LAST_PAGE
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                ITEM_REGULAR -> {
                    val view = inflater.inflate(R.layout.fragment_entry_list_item, parent, false)
                    EntryViewHolder(view, parent as View)
                }
                ITEM_LOAD_MORE -> {
                    val pbar = inflater.inflate(R.layout.view_load_more, parent, false)
                    object: RecyclerView.ViewHolder(pbar) {}
                }
                else -> { // ITEM_LAST_PAGE
                    val lastPage = inflater.inflate(R.layout.view_last_page, parent, false)
                    lastPage.findViewById<TextView>(R.id.last_page_reload).setOnClickListener { refreshEntries(true) }
                    object: RecyclerView.ViewHolder(lastPage) {}
                }
            }
        }

        override fun getItemCount() : Int {
            if (entries.isEmpty()) {
                // seems like we didn't yet load anything in our view, it's probably loading
                // for the first time. Don't show "load more", let's wait while anything shows up.
                return 0
            }

            // we have something in the view already
            return entries.size + 1
        }
    }

}