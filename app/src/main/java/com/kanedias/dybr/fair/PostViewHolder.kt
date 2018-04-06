package com.kanedias.dybr.fair

import android.app.FragmentTransaction
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.entities.Entry
import com.kanedias.dybr.fair.misc.CustomTextView
import com.kanedias.html2md.Html2Markdown
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.Request
import okhttp3.Response
import ru.noties.markwon.Markwon
import ru.noties.markwon.SpannableConfiguration
import ru.noties.markwon.renderer.html.ImageSizeResolverDef
import ru.noties.markwon.spans.AsyncDrawable
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * View holder for showing regular posts in diary view.
 *
 * @see PostListFragment.postRibbon
 * @author Kanedias
 */
class PostViewHolder(iv: View) : RecyclerView.ViewHolder(iv) {

    @BindView(R.id.post_title)
    lateinit var titleView: TextView

    @BindView(R.id.post_date)
    lateinit var dateView: TextView

    @BindView(R.id.post_message)
    lateinit var bodyView: TextView

    @BindViews(R.id.post_edit, R.id.post_delete, R.id.post_more_options)
    lateinit var buttons: List<@JvmSuppressWildcards ImageView>

    /**
     * Entry (post) that this holder represents
     */
    private lateinit var entry: Entry

    /**
     * Listener to show comments of this post
     */
    private val commentShow  = View.OnClickListener { it ->
        val activity = it.context as AppCompatActivity
        val commentsPage = CommentListFragment().apply { entry = this@PostViewHolder.entry }
        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing comment list fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, commentsPage)
                .commit()
    }

    init {
        ButterKnife.bind(this, iv)

        iv.setOnClickListener(commentShow)
    }

    @OnClick(R.id.post_edit)
    fun editPost() {
        val activity = itemView.context as AppCompatActivity
        val postEdit = CreateNewEntryFragment().apply {
            editMode = true
            editEntry = this@PostViewHolder.entry
        }

        activity.supportFragmentManager.beginTransaction()
                .addToBackStack("Showing post edit fragment")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.main_drawer_layout, postEdit)
                .commit()
    }

    @OnClick(R.id.post_delete)
    fun deletePost() {
        val activity = itemView.context as AppCompatActivity

        // delete callback
        val delete = {
            launch(UI) {
                try {
                    async { Network.deleteEntry(entry) }.await()
                    Toast.makeText(activity, R.string.entry_deleted, Toast.LENGTH_SHORT).show()
                    activity.supportFragmentManager.popBackStack()

                    // if we have current tab, refresh it
                    val plPredicate = { it: Fragment -> it is PostListFragment && it.userVisibleHint }
                    val currentTab = activity.supportFragmentManager.fragments.find(plPredicate) as PostListFragment?
                    currentTab?.refreshPosts()
                } catch (ex: Exception) {
                    Network.reportErrors(itemView.context, ex)
                }
            }
        }

        // show confirmation dialog
        MaterialDialog.Builder(itemView.context)
                .title(R.string.confirm_action)
                .content(R.string.are_you_sure)
                .negativeText(android.R.string.no)
                .positiveText(android.R.string.yes)
                .positiveColorRes(R.color.md_red_800)
                .onPositive({ _, _ -> delete() })
                .show()
    }

    /**
     * Show or hide post editing buttons depending on circumstances
     */
    private fun toggleEditButtons(show: Boolean) {
        val visibility = when (show) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        val editTag = itemView.context.getString(R.string.edit_tag)
        buttons.filter { it.tag == editTag }.forEach { it.visibility = visibility }
    }

    /**
     * Called when this holder should be refreshed based on what it must show now
     */
    fun setup(entry: Entry, editable: Boolean) {
        this.entry = entry

        dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(entry.createdAt)
        titleView.text = entry.title

        // content handling
        val mdConfig = SpannableConfiguration.builder(itemView.context)
                .asyncDrawableLoader(DrawableLoader(bodyView))
                .imageSizeResolver(ImageSizeResolverDef())
                .build()
        Markwon.setMarkdown(bodyView, mdConfig, Html2Markdown().parse(entry.content))
        bodyView.movementMethod = CustomTextView.LocalLinkMovementMethod()

        toggleEditButtons(editable)
    }

    inner class DrawableLoader(private val view: TextView): AsyncDrawable.Loader {

        private var imgWait: Deferred<Response>? = null

        override fun cancel(destination: String) {
            imgWait?.cancel(null)
        }

        override fun load(destination: String, drawable: AsyncDrawable) {
            launch(UI) {
                try {
                    val req = Request.Builder().url(destination).build()
                    imgWait = async(CommonPool) { Network.httpClient.newCall(req).execute() }
                    val image = BitmapFactory.decodeStream(imgWait?.await()?.body()?.byteStream())
                    image?.let {
                        val sizedHeight = it.height * view.width / it.width
                        val actual = Bitmap.createScaledBitmap(it, view.width, sizedHeight, false)
                        val result = BitmapDrawable(view.context.resources, actual)
                        result.bounds = Rect(0, 0, view.width, sizedHeight)
                        drawable.result = result
                    }
                } catch (ioex: IOException) {
                    // ignore, just don't load image
                }
            }
        }
    }
}