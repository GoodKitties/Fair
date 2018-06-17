package com.kanedias.dybr.fair.misc

import android.view.MotionEvent
import android.content.Context
import android.text.*
import android.text.style.ClickableSpan
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod


/**
 * This is a text view that prevents clicks on it to be consumed by text view itself.
 *
 * @author Kanedias
 *
 * Created on 05.04.18
 */
class ClickPreventingTextView : TextView {

    private var linkHit: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        setTextIsSelectable(true)
        this.movementMethod = LocalLinkMovementMethod()

        // by default, let clicks on non-links pass through the view, if we want to enable selection
        // we can do this manually afterwards, see EntryViewHolder
        this.isClickable = false
        this.isLongClickable = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        linkHit = false

        // Workaround to make text view through-clickable. If we are clicking a link, handle it
        // If we are not clicking a link, e.g. clicking in any other place inside text view
        // let the text view handle the click itself (false if it's not clickable)
        // The downside is that text can't be selectable in such view, so make it selectable
        // manually when you need to
        val res = super.onTouchEvent(event)
        if (linkHit)
            return linkHit

        return res
    }

    /**
     * Subclasses [ArrowKeyMovementMethod] so it handles selection of text inside this text view
     * but also has parts from [LinkMovementMethod] that are responsible for clicking on links
     *
     * Another special case is indicating whether link was clicked to [ClickPreventingTextView]
     * possessing this movement method
     */
    class LocalLinkMovementMethod: ArrowKeyMovementMethod() {

        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            val action = event.action

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
                var x = event.x.toInt()
                var y = event.y.toInt()

                x -= widget.totalPaddingLeft
                y -= widget.totalPaddingTop

                x += widget.scrollX
                y += widget.scrollY

                val layout = widget.layout
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                val link = buffer.getSpans(off, off, ClickableSpan::class.java)

                if (link.isNotEmpty()) {
                    if (action == MotionEvent.ACTION_UP) {
                        link[0].onClick(widget)
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]))
                    }

                    if (widget is ClickPreventingTextView) {
                        widget.linkHit = true
                    }
                    return true
                }
            }

            return false
        }
    }
}