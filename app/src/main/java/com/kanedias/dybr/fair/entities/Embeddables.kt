package com.kanedias.dybr.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasMany
import moe.banana.jsonapi2.HasOne

/**
 * Helper class to deal with most [HasMany]/[HasOne] links objects
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class LinksObject {
    lateinit var self: String
    lateinit var related: String
}