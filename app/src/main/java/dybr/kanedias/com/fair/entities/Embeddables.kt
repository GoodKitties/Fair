package dybr.kanedias.com.fair.entities

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

/**
 * Whole Draft.JS content body
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class DraftJsContent {

    @field:Json(name = "blocks")
    val blocks = mutableListOf<BlockObject>()

    @field:Json(name = "entryMap")
    val entryMap = mutableMapOf<Int, EntityMapObject>()
}

/**
 * Block object - main building block of text lines
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class BlockObject {
    /**
     * Depth in list, if needed, otherwise 0
     */
    var depth = 0

    var key = 0

    /**
     * entity ranges linking styles from [DraftJsContent.entryMap] with text ranges
     */
    var entityRanges = mutableListOf<EntityRange>()

    /**
     * Entity ranges with inline style (simple bold/italic/strikethrough)
     */
    var inlineEntityRanges = mutableListOf<InlineStyleRange>()

    /**
     * The text of this block. This is what ultimately ends up on user's screen, be it styled or not
     */
    lateinit var text: String

    /**
     * Type of block
     */
    lateinit var type: String // unstyled
}

/**
 * Entity range denoting key from [DraftJsContent.entryMap], its length and offset from the start of the [BlockObject.text]
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class EntityRange {
    /**
     * Styling key in [DraftJsContent.entryMap]
     */
    var key = 0

    /**
     * Length of styled content
     */
    var length = 0

    /**
     * Offset from text starting point
     */
    var offset = 0
}

/**
 * Entity range with inlined style
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class InlineStyleRange {
    var length = 0
    var offset = 0
    lateinit var style: String // BOLD, ITALIC
}

/**
 * Style or link or formatting describing object
 * @author Kanedias
 *
 * Created on 14.01.18
 */
class EntityMapObject {
    lateinit var data: Any
    lateinit var mutability: String // MUTABLE, IMMUTABLE
    lateinit var type: String // LINK
}