package com.kanedias.dybr.fair.misc

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * A recycler view that does not scroll when one of its children requests focus.
 * This is needed if long text views with clickable spans become focused and recycler view
 * scrolls back to the top of them when user doesn't want that.
 *
 * @author Kanedias
 *
 * Created on 19.01.20
 */
class NoScrollRecyclerView : RecyclerView {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun requestChildFocus(child: View?, focused: View?) {
        if (focused is ClickPreventingTextView) {
            // don't scroll to the text view when clicking on clickable spans
            // See https://github.com/GoodKitties/Fair/issues/22
            return
        }

        super.requestChildFocus(child, focused)
    }
}