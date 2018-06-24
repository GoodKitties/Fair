package com.kanedias.dybr.fair

import android.app.FragmentTransaction
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.entities.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
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

    var blog: Blog? = null

    private val entryAdapter = EntryListAdapter()
    private var nextPage = 1
    private var lastPage = false

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(layoutToUse(), container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI(view)
        refreshEntries()
        return view
    }

    open fun layoutToUse() = R.layout.fragment_entry_list

    open fun setupUI(view: View) {
        refresher.setOnRefreshListener { refreshEntries(true) }
        entryRibbon.layoutManager = LinearLayoutManager(activity)
        entryRibbon.adapter = entryAdapter
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
        when (isVisibleToUser && isBlogWritable(blog)) {
            true -> activity.actionButton.show()
            false -> activity.actionButton.hide()
        }
    }

    /**
     * Loads the next page in entry listing. If no pages were loaded before, loads first
     * @param reset reset page counter to first
     */
    fun refreshEntries(reset: Boolean = false) {
        if (blog == null) { // we don't have a blog, just show empty list
            refresher.isRefreshing = false
            return
        }

        if (reset) {
            entryRibbon.smoothScrollToPosition(0)
            nextPage = 1
            lastPage = false
        }

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async(CommonPool) { Network.loadEntries(blog!!, nextPage) }
                updateRibbonPage(success.await(), reset)
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false

            if (isBlogWritable(blog)) {
                activity.actionButton.show()
            }
        }
    }

    /**
     * Update entry ribbon with newly loaded values.
     * @param loaded document with entries and links to pages that was loaded
     * @param reset if true, clear current entries before populating from [loaded]
     */
    private fun updateRibbonPage(loaded: ArrayDocument<Entry>, reset: Boolean) {
        if (loaded.isEmpty()) {
            lastPage = true
            entryAdapter.notifyDataSetChanged()
            return
        }
        nextPage += 1

        entryAdapter.apply {
            if (reset) {
                entries.clear()
            }
            loaded.included
            entries.addAll(loaded)
            notifyDataSetChanged()
        }
    }

    inner class EntryListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val REGULAR_POST = 0
        private val LOAD_MORE = 1
        private val LAST_PAGE = 2

        var entries: MutableList<Entry> = ArrayList()

        override fun getItemViewType(position: Int): Int {
            if (position < entries.size) {
                return REGULAR_POST
            }

            if (lastPage) {
                return  LAST_PAGE
            }

            return LOAD_MORE
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (getItemViewType(position)) {
                REGULAR_POST -> {
                    val entryHolder = holder as EntryViewHolder
                    val entry = entries[position]
                    entryHolder.setup(entry, isBlogWritable(blog))
                }
                LOAD_MORE -> refreshEntries()
                // Nothing needed for LAST_PAGE
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                REGULAR_POST -> {
                    val view = inflater.inflate(R.layout.fragment_entry_list_item, parent, false)
                    EntryViewHolder(view)
                }
                LOAD_MORE -> {
                    val pbar = inflater.inflate(R.layout.view_load_more, parent, false)
                    object: RecyclerView.ViewHolder(pbar) {}
                }
                else -> { // LAST_PAGE
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

    /**
     * Create new entry in current blog. Shows fragment over the main content allowing to enter the
     * title and the text with an editor.
     */
    fun addCreateNewEntryForm() {
        val entryAdd = CreateNewEntryFragment().apply {
            this.blog = this@EntryListFragment.blog!! // at this point we know we have the blog
        }

        fragmentManager!!.beginTransaction()
                .addToBackStack("Showing entry add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, entryAdd)
                .commit()
    }

}