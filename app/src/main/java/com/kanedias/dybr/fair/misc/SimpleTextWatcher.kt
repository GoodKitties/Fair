package com.kanedias.dybr.fair.misc

import android.text.Editable
import android.text.TextWatcher

/**
 * Sometimes I don't want to implement whole 3 methods just for the sake of one.
 *
 * @author Kanedias
 *
 * Created on 27.11.17
 */
open class SimpleTextWatcher : TextWatcher {

    override fun afterTextChanged(str: Editable?) {
    }

    override fun beforeTextChanged(str: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }

    override fun onTextChanged(str: CharSequence?, p1: Int, p2: Int, p3: Int) {
    }
}