package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.OfflineDraft
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.md.handleMarkdown
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import moe.banana.jsonapi2.HasOne
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer


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

    @BindView(R.id.comment_preview)
    lateinit var previewButton: Button

    @BindView(R.id.comment_submit)
    lateinit var submitButton: Button

    private var previewShown = false

    private lateinit var activity: MainActivity

    var editMode = false // create new by default

    /**
     * Comment that is being edited. Only set if [editMode] is `true`
     */
    lateinit var editComment : Comment

    /**
     * Entry this comment belongs to. Only set if [editMode] is `false`
     */
    lateinit var entry: Entry

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.getBoolean("editMode")?.let { editMode = it }
        savedInstanceState?.getSerializable("editComment")?.let { editComment = it as Comment }
        savedInstanceState?.getSerializable("entry")?.let { entry = it as Entry }

        val root = inflater.inflate(R.layout.fragment_create_comment, container, false)
        ButterKnife.bind(this, root)

        activity = context as MainActivity
        if (editMode) {
            populateUI()
        }

        setupTheming(root)

        return root
    }

    private fun setupTheming(root: View) {
        Scoop.getInstance().bind(this, TEXT_BLOCK, root, BackgroundNoAlphaAdapter())
        Scoop.getInstance().bind(this, TEXT, preview)
        Scoop.getInstance().bind(this, TEXT_LINKS, preview, TextViewLinksAdapter())
        Scoop.getInstance().bind(this, TEXT_LINKS, previewButton, TextViewColorAdapter())
        Scoop.getInstance().bind(this, TEXT_LINKS, submitButton, TextViewColorAdapter())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("editMode", editMode)
        when (editMode) {
            true -> outState.putSerializable("editComment", editComment)
            false -> outState.putSerializable("entry", entry)
        }
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
                .neutralColorRes(R.color.green_600)
                .neutralText(R.string.save_offline_draft)
                .onNeutral { _, _ ->
                    DbProvider.helper.draftDao.create(OfflineDraft(contentInput))
                    fragmentManager!!.popBackStack()
                }
                .positiveColorRes(R.color.md_red_900)
                .positiveText(android.R.string.yes)
                .onPositive { _, _ -> fragmentManager!!.popBackStack() }
                .show()
    }

    /**
     * Assemble comment creation request and submit it to the server
     */
    @OnClick(R.id.comment_submit)
    fun submit() {
        // hide keyboard
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)

        // hide edit form, show loading spinner
        val extensions = listOf(StrikethroughExtension.create(), TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(contentInput.text.toString())
        val htmlContent = HtmlRenderer.builder().extensions(extensions).build().render(document)

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

                // if we have current comment list, refresh it
                val clPredicate = { it: Fragment -> it is CommentListFragment }
                val currentTab = fragmentManager!!.fragments.find(clPredicate) as CommentListFragment?
                currentTab?.refreshComments()
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                Network.reportErrors(activity, ex)
            }
        }
    }
}