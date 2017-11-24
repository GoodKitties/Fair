package dybr.kanedias.com.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import dybr.kanedias.com.fair.entities.DiaryEntry
import dybr.kanedias.com.fair.misc.Android
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.IOException

/**
 * @author Kanedias
 *
 * Created on 18.11.17
 */
class PostListFragment: Fragment() {

    @BindView(R.id.post_ribbon)
    lateinit var postRibbon: RecyclerView

    @BindView(R.id.refresher_layout)
    lateinit var refresher: SwipeRefreshLayout

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
            postRibbon.adapter = PostListAdapter()
        }
    }

    inner class PostListAdapter : RecyclerView.Adapter<PostListAdapter.PostViewHolder>() {

        override fun onBindViewHolder(holder: PostViewHolder?, position: Int) {
            val post = posts[position]
            holder!!.titleView.text = post.title
            holder.bodyView.text = post.body
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): PostViewHolder {
            val view = LayoutInflater.from(parent!!.context).inflate(R.layout.fragment_post_list_item, parent, false)
            return PostViewHolder(view)
        }

        override fun getItemCount() = posts.size

        inner class PostViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {
            @BindView(R.id.post_title)
            lateinit var titleView: TextView

            @BindView(R.id.post_message)
            lateinit var bodyView: TextView

            init {
                ButterKnife.bind(this, iv)
                iv.setOnClickListener {} // TODO: show comments fragment
            }

        }
    }
}