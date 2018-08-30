package com.kanedias.dybr.fair

import android.support.design.widget.FloatingActionButton
import android.support.v7.graphics.drawable.DrawerArrowDrawable
import android.support.v7.widget.Toolbar
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

        Scoop.getInstance().addStyleLevel()

        // setup toolbar
        toolbar.title = blog?.title
        toolbar.navigationIcon = DrawerArrowDrawable(activity).apply { progress = 1.0f }
        toolbar.setNavigationOnClickListener { fragmentManager?.popBackStack() }

        // setup FAB
        if (isBlogWritable(blog)) {
            addEntryButton.show()
            addEntryButton.setOnClickListener { addCreateNewEntryForm() }
        }

        setBlogTheme()
    }

    private fun setBlogTheme() {
        Scoop.getInstance().bind(this, TOOLBAR, toolbar)
        Scoop.getInstance().bind(this, TOOLBAR_TEXT, toolbar, ToolbarTextAdapter())
        Scoop.getInstance().bind(this, ACCENT, addEntryButton, FABColorAdapter())
        Scoop.getInstance().bind(this, BACKGROUND, entryRibbon)
        Scoop.getInstance().bindStatusBar(activity, activity, STATUS_BAR)

        blog?.let { applyTheme(it, activity) }
    }

    override fun onDestroy() {
        super.onDestroy()
        Scoop.getInstance().popStyleLevel()
    }
}