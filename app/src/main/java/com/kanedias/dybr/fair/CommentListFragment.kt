package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.CardView
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import com.kanedias.dybr.fair.entities.Comment
import com.kanedias.dybr.fair.entities.Entry
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import moe.banana.jsonapi2.ArrayDocument

/**
 * Fragment which displays selected post and its comments below.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CommentListFragment : Fragment() {

    @BindView(R.id.comment_list_area)
    lateinit var refresher: SwipeRefreshLayout

    @BindView(R.id.post_card)
    lateinit var postCard: CardView

    @BindView(R.id.comment_ribbon)
    lateinit var commentRibbon: RecyclerView

    var entry: Entry? = null

    private val commentAdapter = CommentListAdapter()

    private lateinit var activity: MainActivity

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_comment_list, container, false)
        activity = context as MainActivity

        ButterKnife.bind(this, view)
        setupUI()
        refreshComments()
        return view
    }

    private fun setupUI() {
        refresher.setOnRefreshListener { refreshComments() }
        commentRibbon.layoutManager = LinearLayoutManager(activity)
    }

    fun refreshComments() {
        if (entry == null) // we don't have an entry, just show empty list
            return

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val success = async(CommonPool) { Network.loadComments(entry!!) }
                commentAdapter.comments = success.await()
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
            commentRibbon.adapter = commentAdapter
        }
    }

    /**
     * Adapter for comments list
     */
    inner class CommentListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var comments: ArrayDocument<Comment> = ArrayDocument()

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val postHolder = holder as CommentViewHolder
            val comment = comments[position]
            val profile = comment.profile.get(comments) // it's included
            postHolder.setup(comment, profile)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            val view = inflater.inflate(R.layout.fragment_comment_list_item, parent, false)
            return CommentViewHolder(view)
        }

        override fun getItemCount() = comments.size
    }
}