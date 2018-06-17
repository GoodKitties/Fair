package com.kanedias.dybr.fair.misc

import android.text.method.Touch
import android.view.MotionEvent
import android.content.Context
import android.text.*
import android.text.style.ClickableSpan
import android.widget.TextView
import android.text.method.LinkMovementMethod
import android.util.AttributeSet


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
        this.movementMethod = LocalLinkMovementMethod()
        this.isClickable = false
        this.isLongClickable = false
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        linkHit = false

        val res = super.onTouchEvent(event)
        if (linkHit)
            return true

        return res

    }

    class LocalLinkMovementMethod : LinkMovementMethod() {

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
                } else {
                    Selection.removeSelection(buffer)
                    Touch.onTouchEvent(widget, buffer, event)
                    return false
                }
            }
            return false
        }
    }
}