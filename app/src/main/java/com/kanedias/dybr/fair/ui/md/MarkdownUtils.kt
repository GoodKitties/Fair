package com.kanedias.dybr.fair.ui.md

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import pl.droidsonroids.gif.GifDrawable
import pl.droidsonroids.gif.GifDrawableBuilder
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.spans.AsyncDrawable
import ru.noties.markwon.spans.AsyncDrawableSpan
import java.io.IOException
import kotlin.math.ceil

val CONTENT_TYPE_GIF = MediaType.parse("image/gif")

/**
 * Perform all necessary steps to view Markdown in this text view.
 * Parses input with html2md library and converts resulting markdown to spanned string.
 * @param html input markdown to show
 */
infix fun TextView.handleMarkdown(html: String) {
    val label = this

    launch(UI) {
        // this is computation-intensive task, better do it smoothly
        val span = async {
            val mdConfig = SpannableConfiguration.builder(label.context).asyncDrawableLoader(DrawableLoader(label)).build()
            val spanned = Markwon.markdown(mdConfig, Html2Markdown().parse(html)) as SpannableStringBuilder
            postProcessSpans(label, spanned)

            spanned
        }

        label.text = span.await()
        Markwon.scheduleDrawables(label)
    }


}

/**
 * Post-process spans like MORE or image loading
 */
fun postProcessSpans(view: TextView, spanned: SpannableStringBuilder) {
    postProcessDrawables(spanned, view)
    postProcessMore(spanned, view)

}

/**
 * Post-process MORE statements in the text. They act like `<spoiler>` or `<cut>` tag in some websites
 * @param spanned text to be modified to cut out MORE tags and insert replacements instead of them
 * @param view resulting text view to accept the modified spanned string
 */
fun postProcessMore(spanned: SpannableStringBuilder, view: TextView) {
    while (true) {
        // we need to process all MOREs in the text, start from inner ones, get back to outer in next loops
        val moreDetector = Regex(".*(\\[MORE=(.+?)](.*?)\\[\\/MORE])", RegexOption.DOT_MATCHES_ALL)
        val match = moreDetector.find(spanned) ?: break
        // we have a match, make a replacement

        // get group content out of regex
        val outerRange = match.groups[1]!!.range // from start of [MORE] to the end of [/MORE]
        val moreText = match.groups[2]!!.value // content inside opening tag [MORE=...]
        val innerRange = match.groups[3]!!.range // range between opening and closing tag of MORE
        val innerText = match.groups[3]!!.value // content between opening and closing tag of MORE
        val innerSpanned = spanned.subSequence(innerRange.start, innerRange.start + innerText.length) // contains all spans there

        spanned.replace(outerRange.start, outerRange.endInclusive + 1, moreText) // replace it just with text
        val wrapper = object : ClickableSpan() {

            override fun onClick(widget: View?) {
                // replace wrappers with real previous spans

                val start = spanned.getSpanStart(this)
                val end = spanned.getSpanEnd(this)
                spanned.removeSpan(this)
                spanned.replace(start, end, innerSpanned)

                view.text = spanned
                Markwon.scheduleDrawables(view)
            }
        }
        spanned.setSpan(wrapper, outerRange.start, outerRange.start + moreText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

/**
 * Post-process async drawables in the post. We have to do this because lots of drawables can overload native memory
 * reserved for the application.
 *
 * We replace such drawables with placeholders so if you need to actually load them you can
 *
 * @param spanned text to be modified. We cut out async drawables and put image placeholders instead of them there
 * @param view text view to accept resulting spanned string. On placeholder click show wrapped async drawables
 */
private fun postProcessDrawables(spanned: SpannableStringBuilder, view: TextView) {
    val spans = spanned.getSpans(0, spanned.length, AsyncDrawableSpan::class.java)
    for (span in spans) {
        // skip static images
        if (span.drawable.destination.contains("static")) {
            continue
        }

        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        val spansToWrap = spanned.getSpans(start, end, CharacterStyle::class.java)
        val wrapperImg = ImageSpan(view.context, R.drawable.download_image)
        val wrapperClick = object: ClickableSpan() {

            override fun onClick(widget: View?) {
                // replace wrappers with real previous spans

                // text can be already moved around (due to MORE etc.), refresh start/end variables
                val realStart = spanned.getSpanStart(this)
                val realEnd = spanned.getSpanEnd(this)

                spanned.removeSpan(wrapperImg)
                spanned.removeSpan(this)

                spansToWrap.forEach { spanned.setSpan(it, realStart, realEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE) }
                view.text = spanned
                Markwon.scheduleDrawables(view)
            }

        }

        spansToWrap.forEach { spanned.removeSpan(it) }
        spanned.setSpan(wrapperImg, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanned.setSpan(wrapperClick, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

/**
 * Version without parsing html
 * @see handleMarkdown
 */
infix fun TextView.handleMarkdownRaw(markdown: String) {
    val mdConfig = SpannableConfiguration.builder(this.context).asyncDrawableLoader(DrawableLoader(this)).build()
    val spanned = Markwon.markdown(mdConfig, markdown) as SpannableStringBuilder
    postProcessSpans(this, spanned)

    this.text = spanned
}

/**
 * Class responsible for loading images inside markdown-enabled text-views
 */
class DrawableLoader(private val view: TextView): AsyncDrawable.Loader {

    private val pendingImages: MutableMap<String, Deferred<Drawable?>> = HashMap()

    override fun cancel(destination: String) {
        //pendingImages[destination]?.cancel(null)
    }

    override fun load(destination: String, drawable: AsyncDrawable) {
        launch(UI) {
            try {
                while (view.width == 0) // just inflated
                    delay(500)

                val req = Request.Builder().url(destination).build()
                pendingImages[destination] = async {
                    val resp = Network.httpClient.newCall(req).execute()
                    when (resp.body()!!.contentType()) {
                        CONTENT_TYPE_GIF -> handleGif(resp)
                        else -> handleGeneric(resp)
                    }
                }
                pendingImages[destination]?.await()?.let { drawable.result = it }
            } catch (ioex: IOException) {
                // ignore, just don't load image
            }
        }
    }

    private fun handleGif(resp: Response): Drawable? {
        val buffer = resp.body()!!.bytes()
        val gif = GifDrawable(buffer)
        return if (gif.intrinsicWidth < view.width) {
            gif.apply { bounds = Rect(0, 0, gif.intrinsicWidth, gif.intrinsicHeight) }
        } else {
            val scale = ceil(gif.intrinsicWidth.toDouble() / view.width).toInt()
            val scaled = GifDrawableBuilder().from(buffer).sampleSize(scale).build()
            scaled.apply { bounds = Rect(0, 0, scaled.intrinsicWidth, scaled.intrinsicHeight) }
        }
    }

    private fun handleGeneric(resp: Response) : Drawable? {
        val bitmap = BitmapFactory.decodeStream(resp.body()?.byteStream()) ?: return null
        return if (bitmap.width < view.width) {
            // image is small enough to be inside our view
            val result = BitmapDrawable(view.context.resources, bitmap)
            result.apply { bounds = Rect(0, 0, bitmap.width, bitmap.height) }
        } else {
            // image is big, rescale
            val sizedHeight = bitmap.height * view.width / bitmap.width
            val actual = Bitmap.createScaledBitmap(bitmap, view.width, sizedHeight, false)
            val result = BitmapDrawable(view.context.resources, actual)
            result.apply { bounds = Rect(0, 0, view.width, sizedHeight) }
        }
    }
}