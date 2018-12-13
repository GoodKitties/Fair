package com.kanedias.dybr.fair.ui.md

import android.graphics.drawable.Drawable
import android.preference.PreferenceManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.spans.AsyncDrawable
import ru.noties.markwon.spans.AsyncDrawableSpan
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/**
 * Perform all necessary steps to view Markdown in this text view.
 * Parses input with html2md library and converts resulting markdown to spanned string.
 * @param html input markdown to show
 */
infix fun TextView.handleMarkdown(html: String) {
    val label = this

    label.text = null

    GlobalScope.launch(Dispatchers.Main) {
        // this is computation-intensive task, better do it smoothly
        val span = withContext(Dispatchers.IO) {
            val mdConfig = SpannableConfiguration.builder(label.context).asyncDrawableLoader(DrawableLoader(label)).build()
            val spanned = Markwon.markdown(mdConfig, Html2Markdown().parse(html)) as SpannableStringBuilder
            postProcessSpans(label, spanned)

            spanned
        }

        label.text = span
        Markwon.scheduleDrawables(label)
    }


}

/**
 * Post-process spans like MORE or image loading
 */
fun postProcessSpans(view: TextView, spanned: SpannableStringBuilder) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)

    if (!prefs.getBoolean("auto-load-images", true)) {
        postProcessDrawables(spanned, view)
    }
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

    override fun cancel(destination: String) {
        // we don't need to cancel load as we replace images with placeholders
    }

    override fun load(imageUrl: String, drawable: AsyncDrawable) {
        // resolve URL if it's not absolute
        val base = HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT) ?: return
        val resolved = base.resolve(imageUrl) ?: return

        GlobalScope.launch(Dispatchers.Main) {
            try {
                while (view.width == 0) // just inflated
                    delay(500)

                // initialize bounds
                drawable.initWithKnownDimensions(view.width, 18F)

                // load image
                Glide.with(view)
                        .load(resolved.toString())
                        .apply(RequestOptions()
                                .placeholder(android.R.drawable.progress_indeterminate_horizontal))
                        .into(AsyncDrawableTarget(drawable))

                // image spans are expanded, text views don't invalidate themselves
                view.postInvalidate()

            } catch (ioex: IOException) {
                // ignore, just don't load image
                Log.e("ImageLoader", "Couldn't load image", ioex)
            }
        }
    }

    inner class AsyncDrawableTarget(private val drawable: AsyncDrawable): SimpleTarget<Drawable>() {

        override fun onLoadStarted(placeholder: Drawable?) {
            placeholder?.let { drawable.result = placeholder }
        }

        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
            resource.setBounds(0, 0, resource.intrinsicWidth, resource.intrinsicHeight)
            drawable.result = resource

            if (resource is GifDrawable) {
                resource.start()

            }
        }

        override fun onLoadCleared(placeholder: Drawable?) {
            placeholder?.let { drawable.result = placeholder }
        }
    }
}