package com.kanedias.dybr.fair

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*

/**
 * Fragment that shows list of posts in someone's blog. This is the extension of tab-viewed [EntryListFragment]
 * but in fullscreen with its own floating action button and app bar. Used for opening custom links to
 * dybr.ru from other applications or Fair itself.
 *
 * @author Kanedias
 *
 * Created on 23.06.18
 */
open class EntryListFragmentFull: EntryListFragment() {

    @BindView(R.id.add_entry_button)
    lateinit var addEntryButton: FloatingActionButton

    @BindView(R.id.entry_list_toolbar)
    lateinit var toolbar: Toolbar

    override fun layoutToUse() = R.layout.fragment_entry_list_fullscreen

    override fun setupUI() {
        super.setupUI()

        // setup toolbar
        toolbar.title = profile?.blogTitle
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        // setup FAB
        if (isBlogWritable(profile)) {
            addEntryButton.show()
            addEntryButton.setOnClickListener { addCreateNewEntryForm() }
        }
    }

    override fun setupTheming() {
        // this is a fullscreen fragment, add new style
        styleLevel = Scoop.getInstance().addStyleLevel()

        styleLevel.bind(BACKGROUND, entryRibbon)

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(ACCENT, addEntryButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, addEntryButton, FabIconAdapter())

        styleLevel.bind(ACCENT, fastJumpButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, fastJumpButton, FabIconAdapter())

        styleLevel.bindStatusBar(activity, STATUS_BAR)

        val backgrounds = mapOf<View, Int>(entryRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)
        profile?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }
}