package dybr.kanedias.com.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 * Login call request body. Used in network API logging in procedure.
 * All fields are necessary.
 * Example:
 * ```
 * {
 *     "email": "example@example.com",
 *     "password":"sample"
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 16.11.17
 */
@JsonApi(type = "sessions", policy = Policy.SERIALIZATION_ONLY)
class LoginRequest : Resource() {
        @Json(name = "action")
        val action: String = "login"

        @Json(name = "email")
        lateinit var email: String

        @Json(name = "password")
        lateinit var password: String
}

@JsonApi(type = "sessions", policy = Policy.DESERIALIZATION_ONLY)
class LoginResponse : Resource() {
        @Json(name = "action")
        lateinit var action: String

        @Json(name = "access-token")
        lateinit var accessToken: String
}