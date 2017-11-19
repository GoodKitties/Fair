package dybr.kanedias.com.fair.entities

import java.util.*

/**
 * Diary post/epigraph representation as retrieved from API.
 * Example:
 * ```
 * {
 *   "_id": "5a0f1ea559373c001faf492c",
 *   "updatedAt": "2017-11-18T08:56:04.674Z",
 *   "createdAt": "2017-11-17T17:38:45.264Z",
 *   "d_uri": "random-diary",                   <----- repeated in author
 *   "d_id": "5a0f1e0159373c001faf4927",        <----- repeated in author
 *   "author": {...},
 *   "title": "1, 2, 3",
 *   "body": "Lorem ipsum",
 *   "c_count": 1
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 18.11.17
 */
class DiaryEntry {
    /**
     * Created timestamp of this entry
     */
    lateinit var createdAt: Date

    /**
     * Last updated timestamp of this entry
     */
    lateinit var updatedAt: Date

    /**
     * Author of this entry
     */
    lateinit var author: Author

    /**
     * Title of this entry
     */
    lateinit var title: String
    lateinit var body: String
}