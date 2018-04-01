package com.kanedias.dybr.fair.entities

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
        @field:Json(name = "action")
        lateinit var action: String

        @field:Json(name = "email")
        lateinit var email: String

        @field:Json(name = "password")
        lateinit var password: String

        /**
         * Needed only in confirmation request
         */
        @field:Json(name = "confirmation-token")
        var confirmToken: String? = null
}

@JsonApi(type = "sessions", policy = Policy.DESERIALIZATION_ONLY)
class LoginResponse : Resource() {
        @field:Json(name = "action")
        lateinit var action: String

        @field:Json(name = "access-token")
        lateinit var accessToken: String
}

// Confirmation of registration - not directly used in app
typealias ConfirmRequest = LoginRequest