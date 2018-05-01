package com.kanedias.dybr.fair.ui.md

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.TextView
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.misc.CustomTextView
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
import java.io.IOException
import kotlin.math.ceil

val CONTENT_TYPE_GIF = MediaType.parse("image/gif")

/**
 * Perform all necessary steps to view Markdown in this text view.
 * Parses input with html2md library and converts resulting markdown to spanned string.
 * @param html input markdown to show
 */
infix fun TextView.handleMarkdown(html: String) {
    val mdConfig = SpannableConfiguration.builder(this.context)
            .asyncDrawableLoader(DrawableLoader(this))
            .build()
    Markwon.setMarkdown(this, mdConfig, Html2Markdown().parse(html))
    this.movementMethod = CustomTextView.LocalLinkMovementMethod()
}

/**
 * Version without parsing html
 * @see handleMarkdown
 */
infix fun TextView.handleMarkdownRaw(markdown: String) {
    val mdConfig = SpannableConfiguration.builder(this.context)
            .asyncDrawableLoader(DrawableLoader(this))
            .build()
    Markwon.setMarkdown(this, mdConfig, markdown)
    this.movementMethod = CustomTextView.LocalLinkMovementMethod()
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
                pendingImages[destination] = async(CommonPool) {
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