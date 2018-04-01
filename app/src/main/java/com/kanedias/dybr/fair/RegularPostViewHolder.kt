package com.kanedias.dybr.fair

import android.app.FragmentTransaction
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.entities.Auth
import com.kanedias.dybr.fair.entities.Blog
import com.kanedias.dybr.fair.entities.Entry
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.sufficientlysecure.htmltextview.ClickableTableSpan
import org.sufficientlysecure.htmltextview.HtmlTextView
import org.sufficientlysecure.htmltextview.DrawTableLinkSpan
import java.text.SimpleDateFormat
import java.util.*


/**
 * View holder for showing regular posts in diary view.
 *
 * @see PostListFragment.postRibbon
 * @author Kanedias
 */
class RegularPostViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.post_title)
    lateinit var titleView: TextView

    @BindView(R.id.post_date)
    lateinit var dateView: TextView

    @BindView(R.id.post_message)
    lateinit var bodyView: HtmlTextView

    @BindViews(R.id.post_edit, R.id.post_delete, R.id.post_more_options)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    private lateinit var entry: Entry

    init {
        ButterKnife.bind(this, iv)

        // support loading tables into text views
        bodyView.setClickableTableSpan(ClickableTableSpanImpl())
        val drawTableLinkSpan = DrawTableLinkSpan().apply { tableLinkText = "[tap for table]" }
        drawTableLinkSpan.textColor = iv.context!!.resources.getColor(R.color.md_indigo_200)
        bodyView.setDrawTableLinkSpan(drawTableLinkSpan)

        iv.setOnClickListener {} // TODO: show comments fragment
    }

    @OnClick(R.id.post_edit)
    fun editPost() {
        val activity = itemView.context as AppCompatActivity
        val postEdit = CreateNewEntryFragment().apply {
            editMode = true
            editEntry = this@RegularPostViewHolder.entry
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing post edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, postEdit)
                .commit()
    }

    @OnClick(R.id.post_delete)
    fun deletePost() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            launch(UI) {
                try {
                    async { Network.deleteEntry(entry) }.await()
                    Toast.makeText(activity, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    // if we have current tab, refresh it
                    val plPredicate = { it: Fragment -> it is PostListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(plPredicate) as PostListFragment?
                    currentTab?.refreshPosts()
                } catch (ex: Exception) {
                    Network.reportErrors(itemView.context, ex)
                }
            }
        }

        // show confirmation dialog
        MaterialDialog.Builder(itemView.context)
                .title(R.string.confirm_action)
                .content(R.string.are_you_sure)
                .negativeText(android.R.string.no)
                .positiveText(android.R.string.yes)
                .positiveColorRes(R.color.md_red_800)
                .onPositive({ _, _ -> delete() })
                .show()
    }

    /**
     * Show or hide post editing buttons depending on circumstances
     */
    private fun toggleEditButtons(show: Boolean) {
        val visibility = when (show) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        val editTag = itemView.context.getString(R.string.edit_tag)
        buttons.filter { it.tag == editTag }.forEach { it.visibility = visibility }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    fun setup(entry: Entry, blog: Blog) {
        this.entry = entry

        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entry.createdAt)
        titleView.text = entry.title
        bodyView.setHtml(entry.content)

        toggleEditButtons(blog == Auth.blog)
    }

    /**
     * Show webview in a dialog when tapping on a table placeholder
     */
    inner class ClickableTableSpanImpl: ClickableTableSpan() {
        override fun onClick(widget: View?) {
            val webView = WebView(widget!!.context)
            webView.loadData(getTableHtml(), "text/html", "UTF-8")
            MaterialDialog.Builder(widget.context)
                    .customView(webView, false)
                    .positiveText(android.R.string.ok)
                    .show()
        }

        override fun newInstance(): ClickableTableSpan = ClickableTableSpanImpl()
    }
}