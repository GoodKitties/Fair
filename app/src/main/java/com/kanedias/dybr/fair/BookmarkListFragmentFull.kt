package com.kanedias.dybr.fair

import android.view.View
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import butterknife.BindView
import com.ftinc.scoop.Scoop
import com.kanedias.dybr.fair.dto.*
import com.kanedias.dybr.fair.themes.*
import moe.banana.jsonapi2.ArrayDocument

/**
 * Fragment that shows list of bookmarks of current profile.
 *
 * This is the extension of tab-viewed [EntryListFragment]
 * but in fullscreen with its own floating action button and app bar.
 *
 * @author Kanedias
 *
 * Created on 24.03.19
 */
open class BookmarkListFragmentFull: EntryListFragment() {

    @BindView(R.id.add_entry_button)
    lateinit var addEntryButton: FloatingActionButton

    @BindView(R.id.entry_list_toolbar)
    lateinit var toolbar: Toolbar

    override fun layoutToUse() = R.layout.fragment_entry_list_fullscreen

    override fun setupUI() {
        super.setupUI()

        // setup toolbar
        toolbar.title = getString(R.string.my_bookmarks)
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }
    }

    override fun setupTheming() {
        // this is a fullscreen fragment, add new style
        styleLevel = Scoop.getInstance().addStyleLevel()
        lifecycle.addObserver(styleLevel)

        styleLevel.bind(BACKGROUND, entryRibbon)

        styleLevel.bind(TOOLBAR, toolbar)
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        styleLevel.bind(TOOLBAR_TEXT, toolbar, ToolbarIconsAdapter())

        styleLevel.bind(ACCENT, fastJumpButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, fastJumpButton, FabIconAdapter())

        styleLevel.bind(ACCENT, addEntryButton, BackgroundTintColorAdapter())
        styleLevel.bind(ACCENT_TEXT, addEntryButton, FabIconAdapter())

        styleLevel.bindStatusBar(activity, STATUS_BAR)

        val backgrounds = mapOf<View, Int>(entryRibbon to BACKGROUND/*, toolbar to TOOLBAR*/)
        profile?.let { applyTheme(activity, it, styleLevel, backgrounds) }
    }

    override fun retrieveData(pageNum: Int, starter: Long): () -> ArrayDocument<Entry> = {
        val bookmarks = Network.loadBookmarks(pageNum)
        val entries = bookmarks.mapNotNull { it.entry?.get(it.document) }

        ArrayDocument<Entry>(bookmarks).apply { addAll(entries) }
    }

    /**
     * We don't rely on profile, never skip loading entries
     */
    override fun handleLoadSkip() = false
}