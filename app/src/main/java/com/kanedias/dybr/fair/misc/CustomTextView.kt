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
 * @author Kanedias
 *
 * Created on 05.04.18
 */
class CustomTextView : TextView {

    private var consumeNonUrlClicks = false
    private var linkHit: Boolean = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        linkHit = false
        val res = super.onTouchEvent(event)

        return if (consumeNonUrlClicks) res else linkHit

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

                    if (widget is CustomTextView) {
                        widget.linkHit = true
                    }
                    return true
                } else {
                    Selection.removeSelection(buffer)
                    Touch.onTouchEvent(widget, buffer, event)
                    return false
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }
    }
}