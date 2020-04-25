package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.*

/**
 *
 *
 * @author Kanedias
 *
 * Created on 21.07.19
 */
@JsonApi(type = "reaction-sets", policy = Policy.DESERIALIZATION_ONLY)
class ReactionSetResponse: Resource() {

    /**
     * Name of this reaction set
     */
    @Json(name = "name")
    lateinit var name: String

    /**
     * Person who created this reaction set
     */
    @Json(name = "image-url")
    var author: HasOne<OwnProfile>? = null


    /**
     * Person who created this reaction set
     */
    @Json(name = "reaction-types")
    var reactionTypes: HasMany<ReactionType>? = null
}

typealias ReactionSet = ReactionSetResponse

@JsonApi(type = "reaction-types", policy = Policy.DESERIALIZATION_ONLY)
class ReactionTypeResponse: Resource() {
    /**
     * Name of this reaction type
     */
    @Json(name = "emoji")
    lateinit var emoji: String

    /**
     * Person who created this reaction set
     */
    @Json(name = "image-url")
    var imageUrl: String? = null


    /**
     * Person who created this reaction set
     */
    @Json(name = "reaction-set")
    var reactionSet: HasOne<ReactionSet>? = null
}

typealias ReactionType = ReactionTypeResponse


@JsonApi(type = "reactions", policy = Policy.SERIALIZATION_ONLY)
class CreateReactionRequest: Resource() {

    /**
     * Person who created this reaction
     */
    @Json(name = "author")
    lateinit var author: HasOne<OwnProfile>

    /**
     * Reaction type (emoji) for this reaction
     */
    @Json(name = "reaction-type")
    lateinit var reactionType: HasOne<ReactionType>

    /**
     * Entry this reaction attached to (choice, 1/2)
     */
    @Json(name = "entry")
    var entry: HasOne<Entry>? = null

    /**
     * Comment this reaction attached to (choice, 2/2)
     */
    @Json(name = "comment")
    var comment: HasOne<Comment>? = null

}

@JsonApi(type = "reactions", policy = Policy.DESERIALIZATION_ONLY)
class ReactionResponse: Resource() {

    /**
     * Person who created this reaction
     */
    @Json(name = "author")
    lateinit var author: HasOne<OwnProfile>

    /**
     * Person who created this reaction
     */
    @Json(name = "reaction-type")
    lateinit var reactionType: HasOne<ReactionType>

    /**
     * Entry this reaction attached to (choice, 1/2)
     */
    @Json(name = "entry")
    var entry: HasOne<Entry>? = null

    /**
     * Comment this reaction attached to (choice, 2/2)
     */
    @Json(name = "comment")
    var comment: HasOne<Comment>? = null

}

typealias Reaction = ReactionResponse