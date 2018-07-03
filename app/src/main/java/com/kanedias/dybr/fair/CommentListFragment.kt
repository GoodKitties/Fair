package com.kanedias.dybr.fair

import android.app.FragmentTransaction
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.kanedias.dybr.fair.entities.Comment
import com.kanedias.dybr.fair.entities.Entry
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import moe.banana.jsonapi2.ArrayDocument

/**
 * Fragment which displays selected entry and its comments below.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CommentListFragment : Fragment() {

    @BindView(R.id.comments_toolbar)
    lateinit var toolbar: Toolbar

    @BindView(R.id.comments_list_area)
    lateinit var refresher: SwipeRefreshLayout

    @BindView(R.id.comments_ribbon)
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
        toolbar.title = entry?.title
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        refresher.setOnRefreshListener { refreshComments() }
        commentRibbon.layoutManager = LinearLayoutManager(activity)
    }

    fun refreshComments() {
        if (entry == null) { // we don't have an entry, just show empty list
            refresher.isRefreshing = false
            return
        }

        launch(UI) {
            refresher.isRefreshing = true

            try {
                val entryDemand = async(CommonPool) { Network.loadEntry(entry!!.id) }
                val commentsDemand = async(CommonPool) { Network.loadComments(entry!!) }
                commentAdapter.comments = commentsDemand.await() // refresh comments of this entry
                commentAdapter.entry = entry!!.apply { meta = entryDemand.await().meta } // refresh comment num and participants
                commentRibbon.adapter = commentAdapter
            } catch (ex: Exception) {
                Network.reportErrors(activity, ex)
            }

            refresher.isRefreshing = false
        }
    }

    @OnClick(R.id.add_comment_button)
    fun addComment() {
        val commentAdd = CreateNewCommentFragment().apply {
            this.entry = this@CommentListFragment.entry!! // at this point we know we have the entry
        }

        fragmentManager!!.beginTransaction()
                .addToBackStack("Showing comment add fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .add(R.id.main_drawer_layout, commentAdd)
                .commit()
    }

    /**
     * Adapter for comments list. Top item is an entry being viewed.
     * All views below represent comments to this entry.
     */
    inner class CommentListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_ENTRY = 0
        private val TYPE_COMMENT = 1

        lateinit var comments: ArrayDocument<Comment>
        lateinit var entry: Entry

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is EntryViewHolder -> {
                    holder.setup(entry, false)
                    holder.itemView.isClickable = false
                }
                is CommentViewHolder -> {
                    val comment = comments[position - 1]
                    val profile = comment.profile.get(comments) // it's included
                    holder.setup(comment, profile)
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            if (position == 0) {
                return TYPE_ENTRY
            }
            return TYPE_COMMENT
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(activity)
            return if (viewType == TYPE_ENTRY) {
                val view = inflater.inflate(R.layout.fragment_entry_list_item, parent, false)
                EntryViewHolder(view, allowSelection = true)
            } else {
                val view = inflater.inflate(R.layout.fragment_comment_list_item, parent, false)
                CommentViewHolder(view)
            }
        }

        override fun getItemCount() = comments.size + 1
    }
}