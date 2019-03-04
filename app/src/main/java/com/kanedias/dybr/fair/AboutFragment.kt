package com.kanedias.dybr.fair

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.marcoscg.easyabout.EasyAboutFragment
import com.marcoscg.easyabout.helpers.AboutItemBuilder
import com.marcoscg.easyabout.items.AboutCard
import com.marcoscg.easyabout.items.HeaderAboutItem
import com.marcoscg.easyabout.items.NormalAboutItem


/**
 * @author Kanedias
 *
 * Created on 05.03.19
 */
class AboutFragment: EasyAboutFragment() {

    lateinit var donateHelper: DonateHelper

    override fun configureFragment(ctx: Context, root: View, state: Bundle?) {
        @Suppress("DEPRECATION")
        root.setBackgroundColor(ctx.resources.getColor(R.color.md_grey_900))

        donateHelper = DonateHelper(ctx as AppCompatActivity)

        val appDescItem = HeaderAboutItem.Builder(ctx)
                .setTitle(R.string.app_name)
                .setSubtitle("By ${ctx.getString(R.string.the_maker)}")
                .setIcon(R.mipmap.ic_launcher)
                .build()

        val versionItem = NormalAboutItem.Builder(context)
                .setTitle(R.string.version)
                .setSubtitle("${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                .setIcon(R.drawable.information)

        val licenseItem = AboutItemBuilder.generateLinkItem(ctx, "https://wikipedia.org/wiki/GNU_General_Public_License")
                .setTitle(R.string.license)
                .setSubtitle(R.string.gplv3)
                .setIcon(R.drawable.description)

        val sourceCodeItem = AboutItemBuilder.generateLinkItem(ctx, "https://github.com/GoodKitties/Fair")
                .setTitle(R.string.source_code)
                .setSubtitle(R.string.fork_on_github)
                .setIcon(R.drawable.github)

        val issueItem = AboutItemBuilder.generateLinkItem(ctx, "https://github.com/GoodKitties/Fair/issues/new/choose")
                .setTitle(R.string.report_bug)
                .setSubtitle(R.string.github_issue_tracker)
                .setIcon(R.drawable.bug)

        val aboutAppCard = AboutCard.Builder(context)
                .addItem(appDescItem)
                .addItem(versionItem)
                .addItem(licenseItem)
                .addItem(sourceCodeItem)
                .addItem(issueItem)
                .build()

        val authorDescItem = AboutItemBuilder.generateLinkItem(ctx, "https://www.patreon.com/kanedias")
                .setTitle(R.string.the_maker)
                .setSubtitle(R.string.house_of_maker)
                .setIcon(R.drawable.feather)

        val supportDescItem = NormalAboutItem.Builder(ctx)
                .setTitle(R.string.donate)
                .setIcon(R.drawable.heart)
                .setOnClickListener { donateHelper.donate() }

        val emailDescItem = AboutItemBuilder.generateEmailItem(ctx, "kanedias@xaker.ru")
                .setTitle(R.string.send_email)
                .setIcon(R.drawable.email)

        val aboutAuthorCard = AboutCard.Builder(context)
                .setTitle(R.string.author)
                .addItem(authorDescItem)
                .addItem(supportDescItem)
                .addItem(emailDescItem)
                .build()

        addCard(aboutAppCard)
        addCard(aboutAuthorCard)
    }
}