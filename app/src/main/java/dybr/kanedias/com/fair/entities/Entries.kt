package dybr.kanedias.com.fair.entities

import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 * Entry creation requests
 * @author Kanedias
 *
 * Created on 14.01.18
 */
@JsonApi(type = "entries", policy = Policy.SERIALIZATION_ONLY)
class EntryCreateRequest : Resource() {

}

/**
 * Represents Entry in User -> Profile -> Blog -> Entry -> Comment relation
 * @author Kanedias
 *
 * Created on 14.01.18
 */
@JsonApi(type = "entries", policy = Policy.DESERIALIZATION_ONLY)
class EntryResponse: Resource() {

}

typealias Entry = EntryResponse