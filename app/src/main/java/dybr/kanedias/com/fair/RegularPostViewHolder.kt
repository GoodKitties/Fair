package dybr.kanedias.com.fair

import android.support.v7.widget.RecyclerView
import android.text.Html
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.entities.Entry
import org.sufficientlysecure.htmltextview.ClickableTableSpan
import org.sufficientlysecure.htmltextview.HtmlTextView
import ru.noties.markwon.Markwon
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

    init {
        ButterKnife.bind(this, iv)

        // support loading tables into text views
        bodyView.setClickableTableSpan(ClickableTableSpanImpl())
        val drawTableLinkSpan = DrawTableLinkSpan().apply { tableLinkText = "[tap for table]" }
        drawTableLinkSpan.textColor = iv.context!!.resources.getColor(R.color.md_indigo_200)
        bodyView.setDrawTableLinkSpan(drawTableLinkSpan)

        iv.setOnClickListener {} // TODO: show comments fragment
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

    fun setup(entry: Entry) {
        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entry.createdAt)
        titleView.text = entry.title
        bodyView.setHtml(entry.content)
    }
}