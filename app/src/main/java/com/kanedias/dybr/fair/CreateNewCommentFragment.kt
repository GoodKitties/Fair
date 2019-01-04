package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import com.kanedias.dybr.fair.ui.md.handleMarkdownRaw
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.*
import moe.banana.jsonapi2.HasOne
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.text.SimpleDateFormat
import java.util.*


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

    /**
     * Entry this comment belongs to. Should be always set.
     */
    lateinit var entry: Entry

    var editMode = false // create new by default

    /**
     * Comment that is being edited. Only set if [editMode] is `true`.
     */
    lateinit var editComment : Comment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.getBoolean("editMode")?.let { editMode = it }
        savedInstanceState?.getSerializable("editComment")?.let { editComment = it as Comment }
        savedInstanceState?.getSerializable("entry")?.let { entry = it as Entry }

        val root = inflater.inflate(R.layout.fragment_create_comment, container, false)
        ButterKnife.bind(this, root)

        activity = context as MainActivity
        if (editMode) {
            populateUI()
        } else {
            loadDraft()
        }

        setupTheming(root)

        return root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scoop.getInstance().addStyleLevel()
    }

    override fun onDestroy() {
        super.onDestroy()
        Scoop.getInstance().popStyleLevel(false)
    }

    private fun setupTheming(root: View) {
        Scoop.getInstance().bind(TEXT_BLOCK, root, BackgroundNoAlphaAdapter())
        Scoop.getInstance().bind(TEXT, preview)
        Scoop.getInstance().bind(TEXT_LINKS, preview, TextViewLinksAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, previewButton, TextViewColorAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, submitButton, TextViewColorAdapter())
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
            preview.handleMarkdownRaw(contentInput.text.toString())
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

        // persist draft
        DbProvider.helper.draftDao.create(OfflineDraft(key = "comment,entry=${entry.id}", base = contentInput))
        Toast.makeText(activity, R.string.offline_draft_saved, Toast.LENGTH_SHORT).show()
        fragmentManager!!.popBackStack()
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
        GlobalScope.launch(Dispatchers.Main) {
            try {
                if (editMode) {
                    // alter existing comment
                    comment.id = editComment.id
                    withContext(Dispatchers.IO) { Network.updateComment(comment) }
                    Toast.makeText(activity, R.string.comment_updated, Toast.LENGTH_SHORT).show()
                } else {
                    // create new
                    comment.entry = HasOne(entry)
                    comment.profile = HasOne(Auth.profile)
                    withContext(Dispatchers.IO) { Network.createComment(comment) }
                    Toast.makeText(activity, R.string.comment_created, Toast.LENGTH_SHORT).show()
                }
                fragmentManager!!.popBackStack()

                // if we have current comment list, refresh it
                val clPredicate = { it: Fragment -> it is CommentListFragment }
                val currentTab = fragmentManager!!.fragments.find(clPredicate) as CommentListFragment?
                currentTab?.refreshComments(reset = true)
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                Network.reportErrors(activity, ex)
            }
        }
    }

    @OnClick(R.id.edit_save_offline_draft)
    fun saveDraft() {
        if (contentInput.text.isNullOrEmpty())
            return

        // persist new draft
        DbProvider.helper.draftDao.create(OfflineDraft(base = contentInput))

        // clear the context and show notification
        contentInput.setText("")
        Toast.makeText(context, R.string.offline_draft_saved, Toast.LENGTH_SHORT).show()
    }

    /**
     * Loads a list of drafts from database and shows a dialog with list items to be selected.
     * After offline draft item is selected, this offline draft is deleted from the database and its contents
     * are applied to content of the editor.
     */
    @OnClick(R.id.edit_load_offline_draft)
    fun loadDraft(button: View? = null) {
        val drafts = DbProvider.helper.draftDao.queryBuilder()
                .apply {
                    where()
                            .eq("key", "comment,entry=${entry.id}")
                            .or()
                            .isNull("key")
                }
                .orderBy("createdAt", false)
                .query()

        if (drafts.isEmpty())
            return

        if (button == null && !drafts[0].key.isNullOrBlank()) {
            // was invoked on fragment creation and
            // probably user saved entry previously by clicking "cancel", load it
            popDraftUI(drafts[0])
            return
        }

        val adapter = DraftCommentViewAdapter(drafts)
        val dialog = MaterialDialog.Builder(activity)
                .title(R.string.select_offline_draft)
                .adapter(adapter, LinearLayoutManager(context))
                .build()
        adapter.toDismiss = dialog
        dialog.show()
    }

    /**
     * Loads draft and deletes it from local database
     * @param draft draft to load into UI forms and delete
     */
    private fun popDraftUI(draft: OfflineDraft) {
        contentInput.setText(draft.content)
        Toast.makeText(context, R.string.offline_draft_loaded, Toast.LENGTH_SHORT).show()

        DbProvider.helper.draftDao.deleteById(draft.id)
    }

    /**
     * Recycler adapter to hold list of offline drafts that user saved
     */
    inner class DraftCommentViewAdapter(private val drafts: MutableList<OfflineDraft>) : RecyclerView.Adapter<DraftCommentViewAdapter.DraftCommentViewHolder>() {

        /**
         * Dismiss this if item is selected
         */
        lateinit var toDismiss: MaterialDialog

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftCommentViewHolder {
            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.fragment_edit_form_draft_selection_row, parent, false)
            return DraftCommentViewHolder(v)
        }

        override fun getItemCount() = drafts.size

        override fun onBindViewHolder(holder: DraftCommentViewHolder, position: Int) = holder.setup(position)

        fun removeItem(pos: Int) {
            drafts.removeAt(pos)
            notifyItemRemoved(pos)

            if (drafts.isEmpty()) {
                toDismiss.dismiss()
            }
        }

        /**
         * View holder to show one draft as a recycler view item
         */
        inner class DraftCommentViewHolder(v: View): RecyclerView.ViewHolder(v) {

            private var pos: Int = 0
            private lateinit var draft: OfflineDraft

            @BindView(R.id.draft_date)
            lateinit var draftDate: TextView

            @BindView(R.id.draft_delete)
            lateinit var draftDelete: ImageView

            @BindView(R.id.draft_content)
            lateinit var draftContent: TextView

            init {
                ButterKnife.bind(this, v)
                v.setOnClickListener {
                    popDraftUI(draft)
                    toDismiss.dismiss()
                }

                draftDelete.setOnClickListener {
                    DbProvider.helper.draftDao.deleteById(draft.id)
                    Toast.makeText(context, R.string.offline_draft_deleted, Toast.LENGTH_SHORT).show()
                    removeItem(pos)
                }
            }

            fun setup(position: Int) {
                pos = position
                draft = drafts[position]
                draftDate.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(draft.createdAt)
                draftContent.text = draft.content
            }
        }
    }
}