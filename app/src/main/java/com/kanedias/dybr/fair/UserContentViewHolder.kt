package com.kanedias.dybr.fair

import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author Kanedias
 *
 * Created on 04.01.19
 */
abstract class UserContentViewHolder(iv: View): RecyclerView.ViewHolder(iv) {

    abstract fun getCreationDateView(): TextView
    abstract fun getCreationDate(): Date

    abstract fun getProfileAvatarView(): ImageView
    abstract fun getProfileAvatarUrl(): String?

    abstract fun getProfileId(): String

    open fun setup() {
        getCreationDateView().text = DateUtils.getRelativeTimeSpanString(getCreationDate().time)
        getCreationDateView().setOnClickListener { showFullDate() }

        if (getProfileAvatarUrl() != null && HttpUrl.parse(getProfileAvatarUrl()!!) != null) {
            Glide.with(getProfileAvatarView()).load(getProfileAvatarUrl())
                    .apply(RequestOptions().downsample(DownsampleStrategy.CENTER_INSIDE))
                    .into(getProfileAvatarView())
        }
        getProfileAvatarView().setOnClickListener { showProfile() }
    }

    private fun showFullDate() {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(getCreationDate())
        val toast = Toast.makeText(itemView.context, date, Toast.LENGTH_SHORT)

        val location = IntArray(2)
        getCreationDateView().getLocationOnScreen(location)

        toast.setGravity(Gravity.TOP or Gravity.START, getCreationDateView().right - 25, location[1] - 10)
        toast.show()
    }

    private fun showProfile() {
        val activity = itemView.context as AppCompatActivity

        val dialog = MaterialDialog.Builder(activity)
                .progress(true, 0)
                .cancelable(false)
                .title(R.string.please_wait)
                .content(R.string.loading_profile)
                .build()

        GlobalScope.launch(Dispatchers.Main) {
            dialog.show()

            try {
                val prof = withContext(Dispatchers.IO) { Network.loadProfile(getProfileId()) }
                val profShow = ProfileFragment().apply { profile = prof }
                profShow.show(activity.supportFragmentManager, "Showing user profile fragment")
            } catch (ex: Exception) {
                Network.reportErrors(itemView.context, ex)
            }

            dialog.dismiss()
        }
    }
}