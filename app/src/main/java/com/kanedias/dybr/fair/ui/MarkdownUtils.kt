package com.kanedias.dybr.fair.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Environment
import android.preference.PreferenceManager
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ftinc.scoop.binding.AbstractBinding
import com.kanedias.dybr.fair.BuildConfig
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.themes.TEXT
import com.kanedias.html2md.Html2Markdown
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import ru.noties.markwon.AbstractMarkwonPlugin
import ru.noties.markwon.Markwon
import ru.noties.markwon.MarkwonConfiguration
import ru.noties.markwon.ext.strikethrough.StrikethroughPlugin
import ru.noties.markwon.html.HtmlPlugin
import ru.noties.markwon.image.*
import ru.noties.markwon.image.network.NetworkSchemeHandler
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * Get markdown setup from context.
 *
 * This is stripped-down version for use in notifications and other
 * cases where user interaction is not needed.
 *
 * @param ctx context to initialize from
 */
fun mdRendererFrom(ctx: Context): Markwon {
    return Markwon.builder(ctx)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .build()
}

/**
 * Get commonly-used markdown setup for text views
 */
fun mdRendererFrom(txt: TextView): Markwon {
    return Markwon.builder(txt.context)
            .usePlugin(HtmlPlugin.create())
            .usePlugin(ImagesPlugin.create(txt.context))
            .usePlugin(MarkwonGlidePlugin.create(txt))
            .usePlugin(StrikethroughPlugin.create())
            .build()
}

/**
 * Perform all necessary steps to view Markdown in this text view.
 * Parses input with html2md library and converts resulting markdown to spanned string.
 * @param html input markdown to show
 */
infix fun TextView.handleMarkdown(html: String) {
    val label = this

    GlobalScope.launch(Dispatchers.Main) {
        // this is computation-intensive task, better do it smoothly
        val span = withContext(Dispatchers.IO) {
            val mdContent = Html2Markdown().parse(html)
            val spanned = mdRendererFrom(label).toMarkdown(mdContent) as SpannableStringBuilder
            postProcessSpans(spanned, label)

            spanned
        }

        label.text = span
        AsyncDrawableScheduler.schedule(label)
    }
}

/**
 * Post-process spans like MORE or image loading
 * @param spanned editable spannable to change
 * @param view resulting text view to accept the modified spanned string
 */
fun postProcessSpans(spanned: SpannableStringBuilder, view: TextView) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)

    postProcessDrawables(spanned, view)

    if (!prefs.getBoolean("auto-load-images", true)) {
        postProcessDrawablesLoad(spanned, view)
    }
    postProcessMore(spanned, view)

}

/**
 * Post-process drawables, so you can click on them to see them in full-screen
 * @param spanned editable spannable to change
 * @param view resulting text view to accept the modified spanned string
 */
fun postProcessDrawables(spanned: SpannableStringBuilder, view: TextView) {
    val imgSpans = spanned.getSpans(0, spanned.length, AsyncDrawableSpan::class.java)
    imgSpans.sortBy { spanned.getSpanStart(it) }

    for (img in imgSpans) {
        val start = spanned.getSpanStart(img)
        val end = spanned.getSpanEnd(img)
        val spansToWrap = spanned.getSpans(start, end, CharacterStyle::class.java)
        if (spansToWrap.any { it is ClickableSpan }) {
            // the image is already clickable, we can't replace it
            continue
        }

        val wrapperClick = object : ClickableSpan() {
            override fun onClick(widget: View?) {
                val index = imgSpans.indexOf(img)
                if (index == -1) {
                    // something modified spannable in a way image is no longer here
                    return
                }

                val overlay = ImageShowOverlay(view.context)
                overlay.update(imgSpans[index])

                StfalconImageViewer.Builder<AsyncDrawableSpan>(view.context, imgSpans) { view, span ->
                    val base = HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT) ?: return@Builder
                    val resolved = base.resolve(span.drawable.destination) ?: return@Builder
                    Glide.with(view).load(resolved.toString()).into(view)
                }
                        .withOverlayView(overlay)
                        .withStartPosition(index)
                        .withImageChangeListener { position -> overlay.update(imgSpans[position])}
                        .allowSwipeToDismiss(true)
                        .show()
            }
        }

        spanned.setSpan(wrapperClick, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}

/**
 * Post-process MORE statements in the text. They act like `<spoiler>` or `<cut>` tag in some websites
 * @param spanned text to be modified to cut out MORE tags and insert replacements instead of them
 * @param view resulting text view to accept the modified spanned string
 */
fun postProcessMore(spanned: SpannableStringBuilder, view: TextView) {
    while (true) {
        // we need to process all MOREs in the text, start from inner ones, get back to outer in next loops
        val moreDetector = Regex(".*(\\[MORE=(.+?)](.*?)\\[/MORE])", RegexOption.DOT_MATCHES_ALL)
        val match = moreDetector.find(spanned) ?: break
        // we have a match, make a replacement

        // get group content out of regex
        val outerRange = match.groups[1]!!.range // from start of [MORE] to the end of [/MORE]
        val moreText = match.groups[2]!!.value // content inside opening tag [MORE=...]
        val innerRange = match.groups[3]!!.range // range between opening and closing tag of MORE
        val innerText = match.groups[3]!!.value // content between opening and closing tag of MORE
        val innerSpanned = spanned.subSequence(innerRange.start, innerRange.start + innerText.length) // contains all spans there

        // content of opening tag may be HTML
        val auxMd = Html2Markdown().parse(moreText)
        val auxSpanned = mdRendererFrom(view).toMarkdown(auxMd)

        spanned.replace(outerRange.start, outerRange.endInclusive + 1, auxSpanned) // replace it just with text
        val wrapper = object : ClickableSpan() {

            override fun onClick(widget: View?) {
                // replace wrappers with real previous spans

                val start = spanned.getSpanStart(this)
                val end = spanned.getSpanEnd(this)

                auxSpanned.getSpans(0, auxSpanned.length, Any::class.java).forEach { spanned.removeSpan(it) }
                spanned.removeSpan(this)
                spanned.replace(start, end, innerSpanned)

                view.text = spanned
                AsyncDrawableScheduler.schedule(view)
            }
        }
        spanned.setSpan(wrapper, outerRange.start, outerRange.start + auxSpanned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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
private fun postProcessDrawablesLoad(spanned: SpannableStringBuilder, view: TextView) {
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
                AsyncDrawableScheduler.schedule(view)
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
    val spanned = mdRendererFrom(this).toMarkdown(markdown) as SpannableStringBuilder
    postProcessSpans(spanned, this)

    this.text = spanned
}

/**
 * A plugin to implement image caching by means of using [Glide]
 */
class MarkwonGlidePlugin(private val txt: TextView): AbstractMarkwonPlugin() {

    companion object {
        fun create(txt: TextView): MarkwonGlidePlugin {
            return MarkwonGlidePlugin(txt)
        }
    }

    /**
     * Reroute image loading through the Glide cache
     */
    inner class GlideHandler: SchemeHandler() {

        override fun handle(raw: String, uri: Uri): ImageItem? {
            val base = HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT) ?: return null
            val resolved = base.resolve(raw) ?: return null

            val glide = Glide.with(txt.context)
                    .downloadOnly()
                    .load(resolved.toString())
                    .apply(RequestOptions().centerInside())
                    .submit()

            return ImageItem("image/*", glide.get().inputStream())
        }

    }

    /**
     * Helper class that tints drawable according to current style.
     * In short, this makes placeholders dark in light theme and light in dark one.
     */
    inner class DrawableBinding(drawable: Drawable, topping: Int): AbstractBinding(topping) {

        private val dwRef = WeakReference(drawable)

        override fun update(color: Int) {
            dwRef.get()?.let {
                val wrapped = DrawableCompat.wrap(it)
                DrawableCompat.setTint(wrapped, color)
            }
        }

        override fun unbind() {
            dwRef.clear()
        }

    }

    @Suppress("DEPRECATION") // Keep compatibility with old API
    override fun configureImages(builder: AsyncDrawableLoader.Builder) {
        /*
        // doesn't work yet, see https://github.com/noties/Markwon/issues/115
        builder.placeholderDrawableProvider {
            txt.context.resources.getDrawable(R.drawable.image).mutate().apply {
                txt.styleLevel?.bind(TEXT, DrawableBinding(this, TEXT))
            }
        }
        */
        builder.errorDrawableProvider {
            txt.context.resources.getDrawable(R.drawable.image_broken).apply {
                txt.styleLevel?.bind(TEXT, DrawableBinding(this, TEXT))
            }
        }
        builder.addSchemeHandler(NetworkSchemeHandler.SCHEME_HTTP, GlideHandler())
        builder.addSchemeHandler(NetworkSchemeHandler.SCHEME_HTTPS, GlideHandler())
    }
}

/**
 * Class responsible for showing "Share" and "Download" buttons when viewing images in full-screen.
 */
class ImageShowOverlay(ctx: Context,
                       attrs: AttributeSet? = null,
                       defStyleAttr: Int = 0) : FrameLayout(ctx, attrs, defStyleAttr) {

    @BindView(R.id.overlay_download)
    lateinit var download: ImageView

    @BindView(R.id.overlay_share)
    lateinit var share: ImageView

    init {
        View.inflate(ctx, R.layout.view_image_overlay, this)
        ButterKnife.bind(this)
    }

    fun update(span: AsyncDrawableSpan) {
        val base = HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT) ?: return
        val resolved = base.resolve(span.drawable.destination) ?: return

        // share button: share the image using file provider
        share.setOnClickListener {
            Glide.with(it).asFile().load(resolved.toString()).into(object: SimpleTarget<File>() {

                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    val shareUri = saveToShared(resource)

                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, shareUri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_image_using)))
                }

            })
        }

        // download button: download the image to Download folder on internal SD
        download.setOnClickListener {
            val activity = context as? Activity ?: return@setOnClickListener

            // request SD write permissions if we don't have it already
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                return@setOnClickListener
            }

            Toast.makeText(context, R.string.downloading_image, Toast.LENGTH_SHORT).show()
            Glide.with(it).asFile().load(resolved.toString()).into(object: SimpleTarget<File>() {

                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getMimeTypeOfFile(resource))
                    var name = resolved.pathSegments().last()
                    if (ext!= null && !name.endsWith(ext)) {
                        name += ".$ext"
                    }
                    val downloadedFile = File(downloads, name)
                    downloadedFile.writeBytes(resource.readBytes())

                    val report = context.getString(R.string.image_saved_as) + " ${downloadedFile.absolutePath}"
                    Toast.makeText(context, report, Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    /**
     * Decode the file as bitmap and retrieve its mime type, if it's an image
     * @param file input file to parse
     * @return string-represented mime type of this file
     */
    private fun getMimeTypeOfFile(file: File): String? {
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true

        FileInputStream(file).use {
            BitmapFactory.decodeStream(it, null, opt)
        }

        return opt.outMimeType
    }

    /**
     * Save image file to location that is shared from xml/shared_paths.
     * After that it's possible to share this file to other applications
     * using [FileProvider].
     *
     * @param image image file to save
     * @return Uri that file-provider returns, or null if unable to
     */
    private fun saveToShared(image: File) : Uri? {
        try {
            val sharedImgs = File(context.cacheDir, "shared_images");
            if (!sharedImgs.exists() && !sharedImgs.mkdir()) {
                Log.e("Fair/Markdown", "Couldn't create dir for shared imgs! Path: $sharedImgs")
                return null
            }

            // cleanup old images
            for (oldImg in sharedImgs.listFiles()) {
                if (!oldImg.delete()) {
                    Log.w("Fair/Markdown", "Couldn't delete old image file! Path $oldImg")
                }
            }

            val imgTmpFile = File(sharedImgs, UUID.randomUUID().toString())
            imgTmpFile.writeBytes(image.readBytes())

            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", imgTmpFile)
        } catch (e: IOException) {
            Log.d("Fair/Markdown", "IOException while trying to write file for sharing", e)
        }

        return null
    }
}