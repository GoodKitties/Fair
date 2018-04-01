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
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.entities.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import moe.banana.jsonapi2.ArrayDocument


/**
 * Fragment which displays list of posts in currently viewed blog.
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
class PostListFragment: Fragment() {

    @BindView(R.id.post_ribbon)
    lateinit var postRibbon: RecyclerView

    @BindView(R.id.post_list_area)
    lateinit var refresher: SwipeRefreshLayout

    var blog: Blog? = null

    private val postAdapter = PostListAdapter()

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_post_list, container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI()
        refreshPosts()
        return view
    }

    private fun setupUI() {
        refresher.setOnRefreshListener { refreshPosts() }
        postRibbon.layoutManager = LinearLayoutManager(activity)
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
        when (isVisibleToUser && blog == Auth.blog) {
            false -> activity.actionButton.hide()
            true -> activity.actionButton.show()
        }
    }

    fun refreshPosts() {
        if (blog == null) // we don't have a blog, just show empty list
            return

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async(CommonPool) { Network.loadEntries(blog!!) }
                postAdapter.entries = success.await()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
            postRibbon.adapter = postAdapter

            if (blog == Auth.blog) {
                activity.actionButton.show()
            }
        }
    }

    inner class PostListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var entries: ArrayDocument<Entry> = ArrayDocument()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val postHolder = holder as PostViewHolder
            val entry = entries[position]
            postHolder.setup(entry, blog!!)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.fragment_post_list_item, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount() = entries.size
    }

    /**
     * Adds diary entry on top of post ribbon and scrolls to it
     */
    fun addCreateNewPostForm() {
        val postAdd = CreateNewEntryFragment().apply {
            this.blog = this@PostListFragment.blog!! // at this point we know we have the blog
        }

        fragmentManager!!.beginTransaction()
                .addToBackStack("Showing post add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, postAdd)
                .commit()
    }

}