package dybr.kanedias.com.fair

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.ViewSwitcher
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import dybr.kanedias.com.fair.entities.Blog
import dybr.kanedias.com.fair.entities.EntryCreateRequest
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import ru.noties.markwon.Markwon

/**
 * Fragment responsible for showing create new post form.
 *
 * @see PostListFragment.postRibbon
 * @param iv view inside adapter
 * @param fragment parent blog post list fragment
 * @author Kanedias
 */
class CreateNewPostFragment : Fragment() {

    /**
     * Title of future diary entry
     */
    @BindView(R.id.entry_title_text)
    lateinit var titleInput: EditText

    /**
     * Content of entry
     */
    @BindView(R.id.entry_source_text)
    lateinit var contentInput: EditText

    /**
     * Markdown preview
     */
    @BindView(R.id.entry_markdown_preview)
    lateinit var preview: TextView

    @BindView(R.id.entry_preview_switcher)
    lateinit var previewSwitcher: ViewSwitcher

    private var previewShown = false

    private lateinit var activity: MainActivity

    lateinit var blog: Blog
    lateinit var parent: PostListFragment

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_create_entry, container, false)
        ButterKnife.bind(this, root)

        activity = context as MainActivity

        return root
    }

    /**
     * Handler for clicking on "Preview" button
     */
    @OnClick(R.id.entry_preview)
    fun togglePreview() {
        previewShown = !previewShown

        if (previewShown) {
            //preview.setBackgroundResource(R.drawable.white_border_line) // set border when previewing
            Markwon.setMarkdown(preview, contentInput.text.toString())
            previewSwitcher.showNext()
        } else {
            previewSwitcher.showPrevious()
        }

    }

    /**
     * Handler of all small editing buttons above content input.
     */
    @OnClick(
        R.id.entry_quick_bold, R.id.entry_quick_italic, R.id.entry_quick_underlined, R.id.entry_quick_strikethrough,
        R.id.entry_quick_code, R.id.entry_quick_quote, R.id.entry_quick_number_list, R.id.entry_quick_bullet_list,
        R.id.entry_quick_link, R.id.entry_quick_image
    )
    fun editSelection(v: View) {
        val clipboard = v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val paste = if (clipboard.hasPrimaryClip() && clipboard.primaryClip.itemCount > 0) {
            ""//clipboard.primaryClip.getItemAt(0).text.toString()
        } else {
            ""
        }

        when (v.id) {
            R.id.entry_quick_bold -> insertInCursorPosition("<b>", paste, "</b>")
            R.id.entry_quick_italic -> insertInCursorPosition("<i>", paste, "</i>")
            R.id.entry_quick_underlined -> insertInCursorPosition("<u>", paste, "</u>")
            R.id.entry_quick_strikethrough -> insertInCursorPosition("<s>", paste, "</s>")
            R.id.entry_quick_code -> insertInCursorPosition("```\n", paste, "\n```\n")
            R.id.entry_quick_quote -> insertInCursorPosition("> ", paste)
            R.id.entry_quick_number_list -> insertInCursorPosition("\n1. ", paste, "\n2. \n3. ")
            R.id.entry_quick_bullet_list -> insertInCursorPosition("\n* ", paste, "\n* \n* ")
            R.id.entry_quick_link -> insertInCursorPosition("<a href=\"$paste\">", paste, "</a>")
            R.id.entry_quick_image -> insertInCursorPosition("<img src='$paste'>", paste, ")")
        }
    }

    /**
     * Helper function for inserting quick snippets of markup into the various parts of edited text
     * @param prefix prefix preceding content.
     *          This is most likely non-empty. Cursor is positioned after it in all cases.
     * @param what content to insert.
     *          If it's empty and [suffix] is not, cursor will be positioned here
     * @param suffix suffix after content. Can be empty fairly often. Cursor will be placed after it if [what] is
     *          not empty.
     */
    private fun insertInCursorPosition(prefix: String, what: String, suffix: String = "") {
        var cursorPos = contentInput.selectionStart
        if (cursorPos == -1)
            cursorPos = contentInput.text.length

        val beforeCursor = contentInput.text.substring(0, cursorPos)
        val afterCursor = contentInput.text.substring(cursorPos, contentInput.text.length)

        val beforeCursorWithPrefix = beforeCursor + prefix
        val suffixWithAfterCursor = suffix + afterCursor
        val result = beforeCursorWithPrefix + what + suffixWithAfterCursor
        contentInput.setText(result)

        when {
            // empty string between tags, set cursor between tags
            what.isEmpty() -> contentInput.setSelection(contentInput.text.indexOf(suffixWithAfterCursor, cursorPos))
            // append to text, set cursor to the end of text
            afterCursor.isEmpty() -> contentInput.setSelection(contentInput.text.length)
            // insert inside text, set cursor at the end of paste
            else -> contentInput.setSelection(contentInput.text.indexOf(afterCursor, cursorPos))
        }
    }

    /**
     * Cancel this item editing (with confirmation) and remove it from pending posts
     */
    @Suppress("DEPRECATION") // getColor doesn't work up to API level 23
    @OnClick(R.id.entry_cancel)
    fun cancel() {
        if (titleInput.text.isNullOrEmpty() && contentInput.text.isNullOrEmpty()) {
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

    @OnClick(R.id.entry_submit)
    fun submit() {
        // hide edit form, show loading spinner
        val parser = Parser.builder().build()
        val document = parser.parse(contentInput.text.toString())
        val htmlContent = HtmlRenderer.builder().build().render(document)

        val entry = EntryCreateRequest()
        entry.blog.set(blog)
        entry.apply {
            title = titleInput.text.toString()
            content = htmlContent
        }

        // make http request
        launch(UI) {
            async { Network.createEntry(entry) }.await()
            fragmentManager!!.popBackStack()
            parent.refreshPosts()
        }
    }
}