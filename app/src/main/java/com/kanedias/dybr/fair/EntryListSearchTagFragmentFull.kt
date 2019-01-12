package com.kanedias.dybr.fair

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Filtering fragment which is able to show list of entries filtered by arbitrary option
 *
 * @author Kanedias
 *
 * Created on 23.06.18
 */
class EntryListSearchTagFragmentFull: EntryListFragmentFull() {

    /**
     * Filters for search query
     */
    private lateinit var filters: Map<String, String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        filters = arguments!!.getSerializable("filters") as Map<String, String>

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()

        toolbar.title = "#${filters.entries.first().value}"
    }

    override fun retrieveData(pageNum: Int) = { Network.loadFilteredEntries(filters = this.filters, pageNum = this.currentPage) }

    /**
     * We don't rely on profile, never skip loading entries
     */
    override fun handleLoadSkip() = false
}