package com.kanedias.html2md

/**
 * JNI-dependent class, see Rust crate [html2md](https://crates.io/crates/html2md) for reference.
 *
 * @author Kanedias
 *
 * Created on 01.04.18
 */
class Html2Markdown {

    init {
        System.loadLibrary("html2md")
    }

    /**
     * Parse HTML string to Markdown and return it as output
     */
    external fun parse(html: String): String

}