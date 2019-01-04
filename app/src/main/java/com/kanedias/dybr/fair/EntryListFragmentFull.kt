package com.kanedias.dybr.fair

import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import android.view.View
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
class EntryListFragmentFull: EntryListFragment() {

    @BindView(R.id.add_entry_button)
    lateinit var addEntryButton: FloatingActionButton

    @BindView(R.id.entry_list_toolbar)
    lateinit var toolbar: Toolbar

    override fun layoutToUse() = R.layout.fragment_entry_list_fullscreen

    override fun setupUI(view: View) {
        super.setupUI(view)

        // setup toolbar
        toolbar.title = profile?.blogTitle
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        // setup FAB
        if (isBlogWritable(profile)) {
            addEntryButton.show()
            addEntryButton.setOnClickListener { addCreateNewEntryForm() }
        }

        setBlogTheme(view)
    }

    private fun setBlogTheme(view: View) {
        // this is a fullscreen fragment, add new style
        Scoop.getInstance().addStyleLevel(view)
        Scoop.getInstance().bind(TOOLBAR, toolbar)
        Scoop.getInstance().bind(TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        Scoop.getInstance().bind(TOOLBAR_TEXT, toolbar, ToolbarIconAdapter())
        Scoop.getInstance().bind(ACCENT, addEntryButton, FabColorAdapter())
        Scoop.getInstance().bind(TEXT, addEntryButton, FabIconAdapter())
        Scoop.getInstance().bind(BACKGROUND, entryRibbon)
        Scoop.getInstance().bindStatusBar(activity, STATUS_BAR)

        profile?.let { applyTheme(it, activity) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Scoop.getInstance().popStyleLevel( false)
    }
}