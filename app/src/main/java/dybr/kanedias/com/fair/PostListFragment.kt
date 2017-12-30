package dybr.kanedias.com.fair

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
import dybr.kanedias.com.fair.entities.Author
import dybr.kanedias.com.fair.entities.DiaryEntry
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import ru.noties.markwon.Markwon
import java.util.*


/**
 * Fragment which displays list of posts in currently viewed diary.
 * I can sometimes refer to posts/entries, so just remember that post == entry and they are [DiaryEntry].
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

    private val postAdapter = PostListAdapter()

    /**
     * Address of selected diary, main source of posts
     * Should always be set after instantiation if this fragment.
     */
    private lateinit var uri: String

    private lateinit var activity: MainActivity

    private var entries: List<DiaryEntry> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_post_list, container, false)
        activity = context as MainActivity

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
        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async(CommonPool) { Network.getEntries(uri) }
                entries = success.await()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
            postRibbon.adapter = postAdapter
        }
    }

    inner class PostListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        // first go header views for adding posts, then already ready entries
        val pendingEntries = ArrayList<DiaryEntry>()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            when (holder!!.itemViewType) {
                DiaryEntry.TYPE_PENDING -> {
                    val entry = pendingEntries[position]
                    val pendingHolder = holder as CreateNewPostViewHolder
                    pendingHolder.setup(entry)
                }
                else -> {
                    val postHolder = holder as RegularPostViewHolder
                    val entry = entries[position - pendingEntries.size]
                    postHolder.titleView.text = entry.title
                    Markwon.setMarkdown(postHolder.bodyView, entry.body)
                }
            }

        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return when (viewType) {
                DiaryEntry.TYPE_PENDING -> {
                    val view = inflater.inflate(R.layout.fragment_post_list_add_item, parent, false)
                    CreateNewPostViewHolder(view, this)
                }
                else -> { // TYPE_EXISTING
                    val view = inflater.inflate(R.layout.fragment_post_list_item, parent, false)
                    RegularPostViewHolder(view)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            val headerCount = pendingEntries.size
            return when (position) {
                in 0 until headerCount ->  DiaryEntry.TYPE_PENDING // it's header
                else -> DiaryEntry.TYPE_EXISTING // it's regular post
            }
        }

        override fun getItemCount() = pendingEntries.size + entries.size

    }

    /**
     * Adds diary entry on top of post ribbon and scrolls to it
     */
    fun addCreateNewPostForm() {
        // construct new diary entry
        val entry = DiaryEntry().apply {
            createdAt = Date()
            updatedAt = Date()
            author = Author().apply {
                // not working atm, no profile loading routines
                // authorID = Auth.user.profile.identityId
                // name = Auth.user.name
                uri = this@PostListFragment.uri
            }
        }

        // pending == waiting to be submitted
        postAdapter.pendingEntries.add(entry)
        postAdapter.notifyItemInserted(0)
        postRibbon.scrollToPosition(0)
    }

}