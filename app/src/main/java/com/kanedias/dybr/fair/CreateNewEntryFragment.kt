package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.widget.AppCompatSpinner
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnCheckedChanged
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.ftinc.scoop.Scoop
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.ftinc.scoop.util.Utils
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
     * Permission type of an entry: visible for all, for registered
     * or something else.
     */
    @BindView(R.id.entry_permission_selector)
    lateinit var permissionSpinner: AppCompatSpinner

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

    @BindView(R.id.entry_preview)
    lateinit var previewButton: Button

    @BindView(R.id.entry_submit)
    lateinit var submitButton: Button

    private var previewShown = false

    private lateinit var activity: MainActivity

    var editMode = false // create new by default

    /**
     * Entry that is being edited. Only set if [editMode] is `true`
     */
    lateinit var editEntry: Entry

    /**
     * Blog this entry belongs to. Should be always set.
     */
    lateinit var profile: OwnProfile

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        savedInstanceState?.getBoolean("editMode")?.let { editMode = it }
        savedInstanceState?.getSerializable("editEntry")?.let { editEntry = it as Entry }
        savedInstanceState?.getSerializable("profile")?.let { profile = it as OwnProfile }

        val root = inflater.inflate(R.layout.fragment_create_entry, container, false)
        ButterKnife.bind(this, root)

        activity = context as MainActivity

        setupUI()
        setupTheming(root)

        if (editMode) {
            // we're editing existing entry, populate UI with its contents and settings
            populateEditUI()
        } else {
            // It's new entry, if we canceled it previously, it may be saved to offline draft
            loadDraft()
        }

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

    private fun setupUI() {
        permissionSpinner.adapter = PermissionSpinnerAdapter(listOf(
                RecordAccessItem("private", false),
                RecordAccessItem("registered", true),
                RecordAccessItem("favorites", true),
                null /* visible for all */))
        permissionSpinner.setSelection(3) // select "Visible for all" by default
    }

    private fun setupTheming(root: View) {
        Scoop.getInstance().bind(TEXT_BLOCK, root, BackgroundNoAlphaAdapter())
        Scoop.getInstance().bind(TEXT, titleInput, EditTextAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, titleInput, EditTextLineAdapter())
        Scoop.getInstance().bind(TEXT_OFFTOP, titleInput, EditTextHintAdapter())
        Scoop.getInstance().bind(TEXT, preview)
        Scoop.getInstance().bind(TEXT_LINKS, preview, TextViewLinksAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, previewButton, TextViewColorAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, submitButton, TextViewColorAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, permissionSpinner, SpinnerDropdownColorAdapter())
        Scoop.getInstance().bind(TEXT, draftSwitch, TextViewColorAdapter())
        Scoop.getInstance().bind(TEXT_LINKS, draftSwitch, CheckBoxAdapter())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putBoolean("editMode", editMode)
        when (editMode) {
            false -> outState.putSerializable("profile", profile)
            true -> outState.putSerializable("editEntry", editEntry)
        }
    }

    /**
     * Populates UI elements from entry that is edited.
     * Call once when fragment is initialized.
     */
    private fun populateEditUI() {
        titleInput.setText(editEntry.title)
        draftSwitch.isChecked = editEntry.state == "published"
        // need to convert entry content (html) to Markdown somehow...
        val markdown = Html2Markdown().parse(editEntry.content)
        contentInput.setText(markdown)

        // permission settings, if exist
        when (editEntry.settings?.permissions?.access?.firstOrNull()) {
            RecordAccessItem("private", false) -> permissionSpinner.setSelection(0)
            RecordAccessItem("registered", true) -> permissionSpinner.setSelection(1)
            RecordAccessItem("favorites", true) -> permissionSpinner.setSelection(2)
            else -> permissionSpinner.setSelection(3) // visible for all
        }
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

        // persist draft
        DbProvider.helper.draftDao.create(OfflineDraft(key = "entry,blog=${profile.id}", title = titleInput, base = contentInput))
        Toast.makeText(activity, R.string.offline_draft_saved, Toast.LENGTH_SHORT).show()
        fragmentManager!!.popBackStack()
    }

    /**
     * Assemble entry creation request and submit it to the server
     */
    @OnClick(R.id.entry_submit)
    fun submit() {
        // hide keyboard
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)

        // hide edit form, show loading spinner
        val extensions = listOf(StrikethroughExtension.create(), TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val document = parser.parse(contentInput.text.toString())
        val htmlContent = HtmlRenderer.builder().extensions(extensions).build().render(document)

        val access = when(permissionSpinner.selectedItemPosition) {
            0 -> RecordAccessItem("private", false)
            1 -> RecordAccessItem("registered", true)
            2 -> RecordAccessItem("favorites", true)
            else -> null // visible for all
        }

        val entry = EntryCreateRequest().apply {
            title = titleInput.text.toString()
            state = if (draftSwitch.isChecked) { "published" } else { "draft" }
            content = htmlContent
            settings = RecordSettings(permissions = RecordPermissions(listOfNotNull(access)))
        }

        // make http request
        GlobalScope.launch(Dispatchers.Main) {
            try {
                if (editMode) {
                    // alter existing entry
                    entry.id = editEntry.id
                    withContext(Dispatchers.IO) { Network.updateEntry(entry) }
                    Toast.makeText(activity, R.string.entry_updated, Toast.LENGTH_SHORT).show()
                } else {
                    // create new
                    entry.blog = HasOne(profile) // TODO: fix after migration is complete
                    withContext(Dispatchers.IO) { Network.createEntry(entry) }
                    Toast.makeText(activity, R.string.entry_created, Toast.LENGTH_SHORT).show()
                }
                fragmentManager!!.popBackStack()

                // if we have current tab set, refresh it
                val plPredicate = { it: Fragment -> it is EntryListFragment && it.userVisibleHint }
                val currentTab = fragmentManager!!.fragments.find(plPredicate) as EntryListFragment?
                currentTab?.refreshEntries(reset = true)
            } catch (ex: Exception) {
                // don't close the fragment, just report errors
                Network.reportErrors(activity, ex)
            }
        }
    }

    @OnClick(R.id.edit_save_offline_draft)
    fun saveDraft() {
        if (titleInput.text.isNullOrEmpty() && contentInput.text.isNullOrEmpty())
            return

        // persist new draft
        DbProvider.helper.draftDao.create(OfflineDraft(title = titleInput, base = contentInput))

        // clear the context and show notification
        titleInput.setText("")
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
                            .eq("key", "entry,blog=${profile.id}")
                            .or()
                            .isNull("key")
                }
                .orderBy("createdAt", false)
                .query()

        if (drafts.isEmpty())
            return

        if (button == null && !drafts[0].key.isNullOrBlank()) {
            // probably user saved it by clicking "cancel", load it
            popDraftUI(drafts[0])
            return
        }

        val adapter = DraftEntryViewAdapter(drafts)
        val dialog = MaterialDialog(activity)
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
        titleInput.setText(draft.title)
        contentInput.setText(draft.content)
        Toast.makeText(context, R.string.offline_draft_loaded, Toast.LENGTH_SHORT).show()

        DbProvider.helper.draftDao.deleteById(draft.id)
    }

    /**
     * Recycler adapter to hold list of offline drafts that user saved
     */
    inner class DraftEntryViewAdapter(private val drafts: MutableList<OfflineDraft>) : RecyclerView.Adapter<DraftEntryViewAdapter.DraftEntryViewHolder>() {

        /**
         * Dismiss this if item is selected
         */
        lateinit var toDismiss: MaterialDialog

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftEntryViewHolder {
            val inflater = LayoutInflater.from(context)
            val v = inflater.inflate(R.layout.fragment_edit_form_draft_selection_row, parent, false)
            return DraftEntryViewHolder(v)
        }

        override fun getItemCount() = drafts.size

        override fun onBindViewHolder(holder: DraftEntryViewHolder, position: Int) = holder.setup(position)

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
        inner class DraftEntryViewHolder(v: View): RecyclerView.ViewHolder(v) {

            private var pos: Int = 0
            private lateinit var draft: OfflineDraft

            @BindView(R.id.draft_date)
            lateinit var draftDate: TextView

            @BindView(R.id.draft_delete)
            lateinit var draftDelete: ImageView

            @BindView(R.id.draft_title)
            lateinit var draftTitle: TextView

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
                draftTitle.text = draft.title
            }
        }
    }

    inner class PermissionSpinnerAdapter(items: List<RecordAccessItem?>): ArrayAdapter<RecordAccessItem>(activity, 0, items) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getDropDownView(position, convertView, parent)
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val inflater = LayoutInflater.from(context)
            val view = convertView ?: inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false)

            (view as TextView).let {
                val accessDrawableRes = when(item) {
                    RecordAccessItem("private", false) -> R.drawable.eye_crossed
                    RecordAccessItem("registered", true) -> R.drawable.portrait
                    RecordAccessItem("favorites", true) -> R.drawable.star_filled
                    else -> R.drawable.earth
                }

                @Suppress("DEPRECATION") // minSDK == 14 for now
                val accessDrawable = DrawableCompat.wrap(it.context.resources.getDrawable(accessDrawableRes))
                it.setCompoundDrawablesWithIntrinsicBounds(accessDrawable, null, null, null)
                it.compoundDrawablePadding = Utils.dpToPx(it.context, 4F).toInt()
                it.text = item.toDescription(it.context)

                Scoop.getInstance().bind(TEXT, it, TextViewColorAdapter())
                Scoop.getInstance().bind(TEXT_LINKS, it, TextViewDrawableAdapter())
            }

            return view
        }


    }
}