package com.kanedias.dybr.fair.entities

/**
 * Represents author in API.
 * Example:
 * ```
 * {
 *   "authorID": "5a0f1e0159373c001faf4927",
 *   "slug": "random-diary",
 *   "_id": "5a0f1ea559373c001faf492d",
 *   "name": "Random Author"
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
class Author {
    /**
     * Server-side author id
     */
    lateinit var authorID: String

    /**
     * Author name
     */
    lateinit var name: String

    /**
     * Address path of this author's diary
     */
    lateinit var uri: String
}