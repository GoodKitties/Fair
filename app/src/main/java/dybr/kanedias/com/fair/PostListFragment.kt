package dybr.kanedias.com.fair

import android.app.FragmentTransaction
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import dybr.kanedias.com.fair.entities.*
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

    val postAdapter = PostListAdapter()

    var blog: Blog? = null

    private lateinit var activity: MainActivity

    private var entries: ArrayDocument<Entry> = ArrayDocument()

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

    fun refreshPosts() {
        if (blog == null) // we don't have slug, just show empty list
            return

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async(CommonPool) { Network.getEntries(blog!!) }
                entries = success.await()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
            postRibbon.adapter = postAdapter
        }
    }

    inner class PostListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val postHolder = holder as RegularPostViewHolder
            val entry = entries[position]
            postHolder.setup(entry)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.fragment_post_list_item, parent, false)
            return RegularPostViewHolder(view)
        }

        override fun getItemCount() = entries.size

    }

    /**
     * Adds diary entry on top of post ribbon and scrolls to it
     */
    fun addCreateNewPostForm() {
        val postAdd = CreateNewPostFragment().apply {
            this.blog = this@PostListFragment.blog!!
            this.parent = this@PostListFragment
        }

        fragmentManager!!.beginTransaction()
                .addToBackStack("Showing post add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, postAdd)
                .commit()
    }

}