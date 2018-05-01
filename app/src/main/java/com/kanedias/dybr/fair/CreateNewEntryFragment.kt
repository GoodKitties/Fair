package com.kanedias.dybr.fair

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.entities.Blog
import com.kanedias.dybr.fair.entities.Entry
import com.kanedias.dybr.fair.entities.EntryCreateRequest
import com.kanedias.dybr.fair.ui.md.handleMarkdownRaw
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import moe.banana.jsonapi2.HasOne
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Fragment responsible for showing create entry/edit entry form.
 *
 * @see EntryListFragment.entryRibbon
 * @author Kanedias
 */
class CreateNewEntryFragment : Fragment() {

    /**
     * Title of future diary entry
     */
    @BindView(R.id.entry_title_text)
    lateinit var titleInput: EditText

    /**
     * Content of entry
     */
    @BindView(R.id.source_text)
    lateinit var contentInput: EditText

    /**
     * Markdown preview
     */
    @BindView(R.id.entry_markdown_preview)
    lateinit var preview: TextView

    /**
     * View switcher between preview and editor
     */
    @BindView(R.id.entry_preview_switcher)
    lateinit var previewSwitcher: ViewSwitcher

    @BindView(R.id.entry_draft_switch)
    lateinit var draftSwitch: CheckBox

    private var previewShown = false

    private lateinit var activity: MainActivity

    var editMode = false // create new by default

    /**
     * Entry that is being edited. Only set if [editMode] is `true`
     */
    lateinit var editEntry: Entry

    /**
     * Blog this entry belongs to
     */
    lateinit var blog: Blog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_create_entry, container, false)
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
        titleInput.setText(editEntry.title)
        draftSwitch.isChecked = editEntry.state == "published"
        // need to convert entry content (html) to Markdown somehow...
        val markdown = Html2Markdown().parse(editEntry.content)
        contentInput.setText(markdown)
    }

    /**
     * Handler for clicking on "Preview" button
     */
    @OnClick(R.id.entry_preview)
    fun togglePreview() {
        previewShown = !previewShown

        if (previewShown) {
            //preview.setBackgroundResource(R.drawable.white_border_line) // set border when previewing
            preview.handleMarkdownRaw(contentInput.text.toString())
            previewSwitcher.showNext()
        } else {
            previewSwitcher.showPrevious()
        }

    }

    /**
     * Change text on draft-publish checkbox based on what's currently selected
     */
    @OnCheckedChanged(R.id.entry_draft_switch)
    fun toggleDraftState(publish: Boolean) {
        if (publish) {
            draftSwitch.setText(R.string.publish_entry)
        } else {
            draftSwitch.setText(R.string.make_draft_entry)
        }
    }

    /**
     * Cancel this item editing (with confirmation)
     */
    @Suppress("DEPRECATION") // getColor doesn't work up to API level 23
    @OnClick(R.id.entry_cancel)
    fun cancel() {
        if (editMode || titleInput.text.isNullOrEmpty() && contentInput.text.isNullOrEmpty()) {
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
    @OnClick(R.id.entry_submit)
    fun submit() {
        // hide edit form, show loading spinner
        val extensions = listOf(StrikethroughExtension.create(), TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(contentInput.text.toString())
        val htmlContent = HtmlRenderer.builder().extensions(extensions).build().render(document)

        val entry = EntryCreateRequest()
        entry.apply {
            title = titleInput.text.toString()
            state = if (draftSwitch.isChecked) { "published" } else { "draft" }
            content = htmlContent
        }

        // make http request
        launch(UI) {
            try {
                if (editMode) {
                    // alter existing entry
                    entry.id = editEntry.id
                    async { Network.updateEntry(entry) }.await()
                    Toast.makeText(activity, R.string.entry_updated, Toast.LENGTH_SHORT).show()
                } else {
                    // create new
                    entry.blog = HasOne(blog)
                    async { Network.createEntry(entry) }.await()
                    Toast.makeText(activity, R.string.entry_created, Toast.LENGTH_SHORT).show()
                }
                fragmentManager!!.popBackStack()

                // if we have current tab set, refresh it
                val plPredicate = { it: Fragment -> it is EntryListFragment && it.userVisibleHint }
                val currentTab = fragmentManager!!.fragments.find(plPredicate) as EntryListFragment?
                currentTab?.refreshEntries()
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                Network.reportErrors(activity, ex)
            }
        }
    }
}