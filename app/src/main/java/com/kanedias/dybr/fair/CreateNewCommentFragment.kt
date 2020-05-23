package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.lifecycle.lifecycleScope
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.StyleLevel
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.database.DbProvider
import com.kanedias.dybr.fair.database.entities.OfflineDraft
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*
import com.kanedias.dybr.fair.ui.EditorViews
import com.kanedias.dybr.fair.markdown.handleMarkdownRaw
import com.kanedias.dybr.fair.markdown.markdownToHtml
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.*
import moe.banana.jsonapi2.HasOne
import java.text.SimpleDateFormat
import java.util.*


/**
 * Fragment responsible for creating/updating comments.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class CreateNewCommentFragment : Fragment() {

    companion object {
        const val AUTHOR_LINK = "author-link"
        const val REPLY_TEXT = "reply-text"
    }

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

    private lateinit var styleLevel: StyleLevel

    /**
     * Entry this comment belongs to. Should be always set.
     */
    lateinit var entry: Entry

    var editMode = false // create new by default

    /**
     * Comment that is being edited. Only set if [editMode] is `true`.
     */
    lateinit var editComment : Comment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        savedInstanceState?.getBoolean("editMode")?.let { editMode = it }
        savedInstanceState?.getSerializable("editComment")?.let { editComment = it as Comment }
        savedInstanceState?.getSerializable("entry")?.let { entry = it as Entry }

        val view = inflater.inflate(R.layout.fragment_create_comment, container, false)
        ButterKnife.bind(this, view)

        if (editMode) {
            populateUI()
        } else {
            loadDraft()
            handleMisc()
        }

        setupTheming(view)

        return view
    }

    private fun setupTheming(view: View) {
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(TEXT_BLOCK, view, BackgroundNoAlphaAdapter())
        styleLevel.bind(TEXT, preview)
        styleLevel.bind(TEXT_LINKS, preview, TextViewLinksAdapter())
        styleLevel.bind(TEXT_LINKS, previewButton, TextViewColorAdapter())
        styleLevel.bind(TEXT_LINKS, submitButton, TextViewColorAdapter())
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
        val markdown = Html2Markdown().parseExtended(editComment.content)
        contentInput.setText(markdown)
    }

    /**
     * Handler for clicking on "Preview" button
     */
    @OnClick(R.id.comment_preview)
    fun togglePreview() {
        if (previewSwitcher.displayedChild == 0) {
            preview.handleMarkdownRaw(contentInput.text.toString())
            previewSwitcher.displayedChild = 1
        } else {
            previewSwitcher.displayedChild = 0
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
            requireFragmentManager().popBackStack()
            return
        }

        // persist draft
        DbProvider.helper.draftDao.create(OfflineDraft(key = "comment,entry=${entry.id}", base = contentInput))
        Toast.makeText(requireContext(), R.string.offline_draft_saved, Toast.LENGTH_SHORT).show()
        requireFragmentManager().popBackStack()
    }

    /**
     * Nobody knows about this feature.
     * Cancels dialog and doesn't create any offline draft.
     * @return always true
     */
    @OnLongClick(R.id.comment_cancel)
    fun forceCancel(): Boolean {
        requireFragmentManager().popBackStack()
        return true
    }

    /**
     * Assemble comment creation request and submit it to the server
     */
    @OnClick(R.id.comment_submit)
    fun submit() {
        // hide keyboard
        val imm = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)

        val comment = CreateCommentRequest().apply { content = markdownToHtml(contentInput.text.toString()) }

        val progressDialog = MaterialDialog(requireContext())
                .title(R.string.please_wait)
                .message(R.string.submitting)

        // make http request
        lifecycleScope.launch {
            progressDialog.showThemed(styleLevel)

            try {
                // if we have current comment list, refresh it
                val frgPredicate = { it: Fragment -> it is UserContentListFragment }
                val curFrg = requireFragmentManager().fragments.reversed().find(frgPredicate) as UserContentListFragment?

                if (editMode) {
                    // alter existing comment
                    comment.id = editComment.id
                    withContext(Dispatchers.IO) { Network.updateComment(comment) }
                    Toast.makeText(activity, R.string.comment_updated, Toast.LENGTH_SHORT).show()
                    curFrg?.loadMore(reset = true)
                } else {
                    // create new
                    comment.entry = HasOne(entry)
                    comment.profile = HasOne(Auth.profile)
                    withContext(Dispatchers.IO) { Network.createComment(comment) }
                    Toast.makeText(activity, R.string.comment_created, Toast.LENGTH_SHORT).show()
                    curFrg?.loadMore()
                }
                fragmentManager?.popBackStack()
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                if (isActive) {
                    Network.reportErrors(context, ex)
                }
            }

            progressDialog.dismiss()
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
        val dialog = MaterialDialog(requireContext())
                .title(R.string.select_offline_draft)
                .customListAdapter(adapter)
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
     * Handles miscellaneous conditions, such as:
     * * This fragment was shown due to click on author's nickname
     * * This fragment was shown due to quoting
     *
     */
    private fun handleMisc() {
        // handle click on reply in text selection menu
        arguments?.get(REPLY_TEXT)?.let {
            val selectedText = it as String
            val replyQuoted = selectedText.replace(EditorViews.LINE_START, "> ")
            val withQuote = "${contentInput.text}$replyQuoted\n\n"

            contentInput.setText(withQuote)
            contentInput.setSelection(withQuote.length)
        }

        // handle click on author nickname in comments field
        arguments?.get(AUTHOR_LINK)?.let {
            val entity = it as Authored
            val profile = entity.profile.get(entity.document)

            val withAuthorLink = when(entity) {
                is Comment -> "${contentInput.text}[${profile.nickname}](#${entity.id}), "
                else -> "${contentInput.text}[${profile.nickname}](#), "
            }
            contentInput.setText(withAuthorLink)
            contentInput.setSelection(withAuthorLink.length)
        }
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