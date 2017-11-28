package dybr.kanedias.com.fair

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import dybr.kanedias.com.fair.entities.DiaryEntry
import dybr.kanedias.com.fair.misc.Android
import kotlinx.coroutines.experimental.*
import ru.noties.markwon.Markwon
import java.io.IOException


/**
 * @author Kanedias
 *
 * Created on 18.11.17
 */
class PostListFragment: Fragment() {

    @BindView(R.id.post_ribbon)
    lateinit var postRibbon: RecyclerView

    @BindView(R.id.post_list_area)
    lateinit var refresher: SwipeRefreshLayout

    private val postAdapter = PostListAdapter()

    /**
     * Address of selected diary, main source of posts
     * Should always be set after instantiation if this fragment.
     */
    lateinit var uri: String

    private var posts: List<DiaryEntry> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_post_list, container, false)
        ButterKnife.bind(this, view)
        setupUI()
        retrievePosts()
        return view
    }

    private fun setupUI() {
        refresher.setOnRefreshListener { retrievePosts() }
        postRibbon.layoutManager = LinearLayoutManager(activity)
    }

    private fun retrievePosts() {
        launch(Android) {
            refresher.isRefreshing = true

            try {
                val postFuture = async(CommonPool) { Network.getEntries(uri) }
                posts = postFuture.await()
            } catch (ioex: IOException) {
                val errorText = getString(R.string.error_connecting)
                Toast.makeText(this@PostListFragment.context, "$errorText: ${ioex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }

            refresher.isRefreshing = false
            postRibbon.adapter = postAdapter
        }
    }

    inner class PostListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        // first go header views for adding posts, then already ready entries
        val pendingPosts = ArrayList<DiaryEntry>()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            when (holder!!.itemViewType) {
                DiaryEntry.TYPE_PENDING -> {
                    val pendingHolder = holder as CreateNewPostViewHolder
                    val post = pendingPosts[position]
                }
                else -> {
                    val postHolder = holder as RegularPostViewHolder
                    val post = posts[position - pendingPosts.size]
                    postHolder.titleView.text = post.title
                    Markwon.setMarkdown(postHolder.bodyView, post.body)
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent!!.context)
            return when (viewType) {
                DiaryEntry.TYPE_PENDING -> {
                    val view = inflater.inflate(R.layout.fragment_post_list_add_item, parent, false)
                    CreateNewPostViewHolder(view)
                }
                else -> { // TYPE_EXISTING
                    val view = inflater.inflate(R.layout.fragment_post_list_item, parent, false)
                    RegularPostViewHolder(view)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            val headerCount = pendingPosts.size
            return when (position) {
                in 0 until headerCount ->  DiaryEntry.TYPE_PENDING // it's header
                else -> DiaryEntry.TYPE_EXISTING // it's regular post
            }
        }

        override fun getItemCount() = pendingPosts.size + posts.size

    }

    fun showAddEntryForm() {
        postAdapter.pendingPosts.add(DiaryEntry())
        postAdapter.notifyItemInserted(0)
        postRibbon.scrollToPosition(0)
    }

}