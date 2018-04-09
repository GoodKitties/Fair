package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.entities.*
import com.kanedias.dybr.fair.ui.handleMarkdown
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import moe.banana.jsonapi2.HasOne
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import ru.noties.markwon.Markwon

/**
 * Frag,emt responsible for creating/updating comments.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CreateNewCommentFragment : Fragment() {

    /**
     * Content of comment
     */
    @BindView(R.id.source_text)
    lateinit var contentInput: EditText

    /**
     * Markdown preview
     */
    @BindView(R.id.comment_markdown_preview)
    lateinit var preview: TextView

    /**
     * View switcher between preview and editor
     */
    @BindView(R.id.comment_preview_switcher)
    lateinit var previewSwitcher: ViewSwitcher

    private var previewShown = false

    private lateinit var activity: MainActivity

    var editMode = false // create new by default

    /**
     * Comment that is being edited. Only set if [editMode] is `true`
     */
    lateinit var editComment : Comment

    /**
     * Entry this comment belongs to
     */
    lateinit var entry: Entry

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_create_comment, container, false)
        ButterKnife.bind(this, root)

        activity = context as MainActivity
        if (editMode) {
            populateUI()
        }

        return root
    }

    /**
     * Populates UI elements from entry that is edited.
     * Call once when fragment is initialized.
     */
    private fun populateUI() {
        // need to convert entry content (html) to Markdown somehow...
        val markdown = Html2Markdown().parse(editComment.content)
        contentInput.setText(markdown)
    }

    /**
     * Handler for clicking on "Preview" button
     */
    @OnClick(R.id.comment_preview)
    fun togglePreview() {
        previewShown = !previewShown

        if (previewShown) {
            //preview.setBackgroundResource(R.drawable.white_border_line) // set border when previewing
            preview.handleMarkdown(contentInput.text.toString())
            previewSwitcher.showNext()
        } else {
            previewSwitcher.showPrevious()
        }

    }

    /**
     * Cancel this item editing (with confirmation)
     */
    @Suppress("DEPRECATION") // getColor doesn't work up to API level 23
    @OnClick(R.id.comment_cancel)
    fun cancel() {
        if (editMode || contentInput.text.isNullOrEmpty()) {
            // entry has empty title and content, canceling right away
            fragmentManager!!.popBackStack()
            return
        }

        // entry has been written to, delete with confirmation only
        MaterialDialog.Builder(activity)
                .title(android.R.string.dialog_alert_title)
                .content(R.string.are_you_sure)
                .negativeText(android.R.string.no)
                .positiveColorRes(R.color.md_red_900)
                .positiveText(android.R.string.yes)
                .onPositive { _, _ -> fragmentManager!!.popBackStack() }
                .show()
    }

    /**
     * Assemble entry creation request and submit it to the server
     */
    @OnClick(R.id.comment_submit)
    fun submit() {
        // hide edit form, show loading spinner
        val parser = Parser.builder().build()
        val document = parser.parse(contentInput.text.toString())
        val htmlContent = HtmlRenderer.builder().build().render(document)

        val comment = CreateCommentRequest().apply { content = htmlContent }

        // make http request
        launch(UI) {
            try {
                if (editMode) {
                    // alter existing comment
                    comment.id = editComment.id
                    async { Network.updateComment(comment) }.await()
                    Toast.makeText(activity, R.string.comment_updated, Toast.LENGTH_SHORT).show()
                } else {
                    // create new
                    comment.entry = HasOne(entry)
                    comment.profile = HasOne(Auth.profile)
                    async { Network.createComment(comment) }.await()
                    Toast.makeText(activity, R.string.comment_created, Toast.LENGTH_SHORT).show()
                }
                fragmentManager!!.popBackStack()

                // if we have current tab set, refresh it
                val elPredicate = { it: Fragment -> it is EntryListFragment && it.userVisibleHint }
                val currentTab = fragmentManager!!.fragments.find(elPredicate) as EntryListFragment?
                currentTab?.refreshEntries()
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                Network.reportErrors(activity, ex)
            }
        }
    }
}