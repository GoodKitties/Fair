package com.kanedias.html2md

/**
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class Html2Markdown {

    init {
        System.loadLibrary("html2md")
    }

    external fun parse(html: String): String

}