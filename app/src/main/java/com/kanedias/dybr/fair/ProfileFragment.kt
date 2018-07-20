package com.kanedias.dybr.fair

import android.app.Dialog
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.kanedias.dybr.fair.entities.OwnProfile
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.Request
import ru.noties.markwon.spans.AsyncDrawable
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Kanedias
 *
 * Created on 20.07.18
 */
class ProfileFragment: DialogFragment() {

    @BindView(R.id.author_avatar)
    lateinit var authorAvatar: ImageView

    @BindView(R.id.author_name)
    lateinit var authorName: TextView

    @BindView(R.id.author_registration_date)
    lateinit var registrationDate: TextView

    @BindView(R.id.author_blog)
    lateinit var authorBlog: TextView

    lateinit var profile: OwnProfile

    private lateinit var activity: MainActivity

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity = context as MainActivity

        val view = activity.layoutInflater.inflate(R.layout.fragment_profile, null)
        ButterKnife.bind(this, view)
        setupUI()

        return MaterialDialog.Builder(activity)
                .title(R.string.view_profile)
                .customView(view, true)
                .neutralText(android.R.string.ok)
                .build()
    }

    private fun setupUI() {
        authorName.text = profile.nickname
        registrationDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(profile.createdAt)

        val blog = profile.blogs.get(profile.document)?.first()
        if (blog != null) {
            @Suppress("DEPRECATION") // we need to support API < 24
            authorBlog.text = Html.fromHtml("<a href='https://dybr.ru/blog/${blog.slug}'>${blog.title}</a>")
            authorBlog.movementMethod = LinkMovementMethod.getInstance()
        } else {
            authorBlog.text = ""
        }

        val avatarUrl = profile.settings?.avatar
        if (!avatarUrl.isNullOrBlank()) {
            // load avatar asyncchronously
            launch(UI) {
                try {
                    val req = Request.Builder().url(avatarUrl!!).build()
                    val bitmap = async(CommonPool) {
                        val resp = Network.httpClient.newCall(req).execute()
                        BitmapFactory.decodeStream(resp.body()?.byteStream())
                    }.await()
                    authorAvatar.setImageBitmap(bitmap)
                } catch (ioex: IOException) {
                    Network.reportErrors(activity, ioex)
                }
            }
        } else {
            @Suppress("DEPRECATION") // we need to support API < 26
            authorAvatar.setImageDrawable(ColorDrawable(activity.resources.getColor(R.color.md_grey_600)))
        }
    }
}