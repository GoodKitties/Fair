package com.kanedias.dybr.fair.themes

import android.widget.TextView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.internal.button.DialogActionButton
import com.ftinc.scoop.StyleLevel
import com.ftinc.scoop.adapters.TextViewColorAdapter
import com.kanedias.dybr.fair.R

fun MaterialDialog.showThemed(styleLevel: StyleLevel) {

    val dialogTitle = this.view.titleLayout.findViewById(R.id.md_text_title) as TextView
    val dialogMessage = this.view.contentLayout.findViewById(R.id.md_text_message) as? TextView
    val posButton = this.view.buttonsLayout?.findViewById<DialogActionButton>(R.id.md_button_positive)
    val neuButton = this.view.buttonsLayout?.findViewById<DialogActionButton>(R.id.md_button_neutral)
    val negButton = this.view.buttonsLayout?.findViewById<DialogActionButton>(R.id.md_button_negative)

    styleLevel.bind(TEXT_BLOCK, this.view, BackgroundNoAlphaAdapter())
    styleLevel.bind(TEXT_HEADERS, dialogTitle, TextViewColorAdapter())
    styleLevel.bind(TEXT, dialogMessage, TextViewColorAdapter())

    posButton?.let { styleLevel.bind(TEXT_LINKS, it, MaterialDialogButtonAdapter()) }
    neuButton?.let { styleLevel.bind(TEXT_LINKS, it, MaterialDialogButtonAdapter()) }
    negButton?.let { styleLevel.bind(TEXT_LINKS, it, MaterialDialogButtonAdapter()) }

    show()
}