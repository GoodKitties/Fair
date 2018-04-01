package com.kanedias.dybr.fair.misc

/**
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class Html2MdParser {

    init {
        System.loadLibrary("html2md")
    }

    external fun parse(html: String): String

}