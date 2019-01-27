package com.kanedias.dybr.fair.ui

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.kanedias.dybr.fair.BuildConfig
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.ui.EditorViews.Companion.PERMISSION_REQUEST_STORAGE_FOR_IMAGE_UPLOAD
import com.kanedias.html2md.Html2Markdown
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.*
import okhttp3.HttpUrl
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.spans.AsyncDrawable
import ru.noties.markwon.spans.AsyncDrawableSpan
import java.io.*
import java.util.*

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
            val mdConfig = SpannableConfiguration.builder(label.context).asyncDrawableLoader(DrawableLoader(label)).build()
            val spanned = Markwon.markdown(mdConfig, Html2Markdown().parse(html)) as SpannableStringBuilder
            postProcessSpans(spanned, label)

            spanned
        }

        label.text = span
        Markwon.scheduleDrawables(label)
    }
}

/**
 * Post-process spans like MORE or image loading
 */
fun postProcessSpans(spanned: SpannableStringBuilder, view: TextView) {
    val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)

    postProcessDrawables(spanned, view)

    if (!prefs.getBoolean("auto-load-images", true)) {
        postProcessDrawablesLoad(spanned, view)
    }
    postProcessMore(spanned, view)

}

fun postProcessDrawables(spanned: SpannableStringBuilder, view: TextView) {
    val spans = spanned.getSpans(0, spanned.length, AsyncDrawableSpan::class.java)
    for (span in spans) {
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        val spansToWrap = spanned.getSpans(start, end, CharacterStyle::class.java)
        if (spansToWrap.any { it is ClickableSpan }) {
            // the image is clickable, we can't replace it
            continue
        }

        val wrapperClick = object : ClickableSpan() {
            override fun onClick(widget: View?) {
                val overlay = ImageShowOverlay(view.context)
                overlay.update(spans[0])

                StfalconImageViewer.Builder<AsyncDrawableSpan>(view.context, spans) { view, span ->
                    val base = HttpUrl.parse(Network.MAIN_DYBR_API_ENDPOINT) ?: return@Builder
                    val resolved = base.resolve(span.drawable.destination) ?: return@Builder
                    Glide.with(view).load(resolved.toString()).into(view)
                }
                        .withOverlayView(overlay)
                        .withImageChangeListener { position -> overlay.update(spans[position])}
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
    postProcessSpans(spanned, this)

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
                                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                                .downsample(DownsampleStrategy.CENTER_INSIDE))
                        .into(AsyncDrawableTarget(view.width, drawable))

            } catch (ioex: IOException) {
                // ignore, just don't load image
                Log.e("ImageLoader", "Couldn't load image", ioex)
            }
        }
    }

    inner class AsyncDrawableTarget(width: Int, private val drawable: AsyncDrawable): SimpleTarget<Drawable>(width, Target.SIZE_ORIGINAL) {

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

        download.setOnClickListener {
            val activity = context as? Activity ?: return@setOnClickListener

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
                return@setOnClickListener
            }

            Toast.makeText(context, R.string.downloading_image, Toast.LENGTH_SHORT).show()
            Glide.with(it).asFile().load(resolved.toString()).into(object: SimpleTarget<File>() {

                override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(getMimeTypeOfFile(context, resource))
                    val name = "${resolved.pathSegments().last()}.$ext"
                    val downloadedFile = File(downloads, name)
                    downloadedFile.writeBytes(resource.readBytes())

                    val report = context.getString(R.string.image_saved_as) + " ${downloadedFile.absolutePath}"
                    Toast.makeText(context, report, Toast.LENGTH_SHORT).show()
                }

            })
        }
    }

    private fun getMimeTypeOfFile(ctx: Context, file: File): String? {
        val opt = BitmapFactory.Options()
        opt.inJustDecodeBounds = true

        FileInputStream(file).use {
            BitmapFactory.decodeStream(it, null, opt)
        }

        return opt.outMimeType
}

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