package com.kanedias.dybr.fair.markdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kanedias.dybr.fair.Network
import com.kanedias.dybr.fair.R
import com.kanedias.dybr.fair.misc.styleLevel
import com.kanedias.dybr.fair.themes.TEXT
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.DrawableUtils
import io.noties.markwon.image.glide.GlideImagesPlugin
import java.nio.charset.Charset
import java.security.MessageDigest


/**
 * Glide store that scales up smilies
 *
 * @author Kanedias
 *
 * Created on 2020-01-07
 */
class GlideGifSupportStore(view: TextView): GlideImagesPlugin.GlideStore {

    private val ctx = view.context

    private val requestManager = Glide.with(ctx)
        .applyDefaultRequestOptions(
            RequestOptions()
                .centerInside()
                .override(ctx.resources.displayMetrics.widthPixels, Target.SIZE_ORIGINAL)
                .transform(ScaleToDensity(ctx))
                .placeholder(wrapStyleDrawable(view, R.drawable.image))
                .error(wrapStyleDrawable(view, R.drawable.image_broken)))

    override fun cancel(target: Target<*>) = requestManager.clear(target)

    override fun load(drawable: AsyncDrawable) =
            requestManager.load(Network.resolve(drawable.destination).toString())


    /**
     * scales small images to match density of the screen. Mainly needed for smiley pictures.
     */
    class ScaleToDensity(ctx: Context): BitmapTransformation() {
        companion object {
            const val ID = "com.kanedias.holywarsoo.markdown.ScaleToDensity"
            val ID_BYTES = ID.toByteArray(Charset.forName("UTF-8"))
        }

        private val density = ctx.resources.displayMetrics.density

        override fun transform(pool: BitmapPool, toTransform: Bitmap, outWidth: Int, outHeight: Int): Bitmap {
            if (outHeight > 100) {
                return toTransform
            }

            val scaledWidth = (toTransform.width * density).toInt()
            val scaledHeight = (toTransform.height * density).toInt()
            return Bitmap.createScaledBitmap(toTransform, scaledWidth, scaledHeight, true)
        }

        override fun equals(other: Any?) = other is ScaleToDensity

        override fun hashCode() = ID.hashCode()

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update(ID_BYTES)
        }
    }

    /**
     * Style placeholder drawables
     */
    @Suppress("DEPRECATION")
    fun wrapStyleDrawable(view: View, image: Int): Drawable {
        val drawable = view.context.resources.getDrawable(image).mutate()
        DrawableUtils.applyIntrinsicBoundsIfEmpty(drawable)
        view.styleLevel?.bind(TEXT, DrawableBinding(drawable, TEXT))
        return drawable
    }
}