package dybr.kanedias.com.fair

import android.content.ClipboardManager
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import dybr.kanedias.com.fair.misc.Android
import dybr.kanedias.com.fair.misc.SimpleTextWatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import ru.noties.markwon.Markwon
import java.util.concurrent.TimeUnit

/**
 * View holder responsible for showing create new post form.
 * This is shown as recycler view element.
 *
 * @see PostListFragment.postRibbon
 * @author Kanedias
 */
class CreateNewPostViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

    /**
     * Title of future diary entry
     */
    @BindView(R.id.entry_title_text)
    lateinit var titleInput: EditText

    /**
     * Content of entry
     */
    @BindView(R.id.entry_source_text)
    lateinit var bodyInput: EditText

    /**
     * Markdown preview
     */
    @BindView(R.id.entry_markdown_preview)
    lateinit var preview: TextView

    init {
        ButterKnife.bind(this, iv)

        // initialize what we can before binding
        bodyInput.addTextChangedListener(object: SimpleTextWatcher() {

            private var routine: Job? = null

            override fun afterTextChanged(str: Editable?) {
                if (TextUtils.isEmpty(str))
                    return


                if (routine != null) {
                    routine!!.cancel()
                }

                routine = launch(Android) {
                    for (countdown in 3 downTo 0) {
                        delay(1, TimeUnit.SECONDS)
                        val notification = iv.context.getString(R.string.previewing_in_n_seconds)
                        preview.setBackgroundResource(0) // clear border
                        preview.text = String.format(notification, countdown)
                    }

                    preview.setBackgroundResource(R.drawable.white_border_line) // set border when previewing
                    Markwon.setMarkdown(preview, str.toString())
                }
            }
        })
    }

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
            R.id.entry_quick_link -> insertInCursorPosition("[$paste](", paste, ")")
            R.id.entry_quick_image -> insertInCursorPosition("![$paste](", paste, ")")
        }
    }

    private fun insertInCursorPosition(prefix: String, what: String, suffix: String = "") {
        var cursorPos = bodyInput.selectionStart
        if (cursorPos == -1)
            cursorPos = bodyInput.text.length

        val beforeCursor = bodyInput.text.substring(0, cursorPos)
        val afterCursor = bodyInput.text.substring(cursorPos, bodyInput.text.length)

        val beforeCursorWithPrefix = beforeCursor + prefix
        val suffixWithAfterCursor = suffix + afterCursor
        bodyInput.setText(beforeCursorWithPrefix + what + suffixWithAfterCursor)

        when {
            // empty string between tags, set cursor between tags
            what.isEmpty() -> bodyInput.setSelection(bodyInput.text.indexOf(suffixWithAfterCursor, cursorPos))
            // append to text, set cursor to the end of text
            afterCursor.isEmpty() -> bodyInput.setSelection(bodyInput.text.length)
            // insert inside text, set cursor at the end of paste
            else -> bodyInput.setSelection(bodyInput.text.indexOf(afterCursor, cursorPos))
        }
    }
}