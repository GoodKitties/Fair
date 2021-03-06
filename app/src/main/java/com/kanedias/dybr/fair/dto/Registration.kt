package com.kanedias.dybr.fair.dto

import com.squareup.moshi.Json
import moe.banana.jsonapi2.HasMany
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource

/**
 * Register call request body. Used in network API registration procedure.
 * All fields are necessary.
 * Example:
 * ```
 * {
 *   "data": {
 *     "type": "users",
 *     "attributes": {
 *       "email": "test@example.com",
 *       "password": "testpass",
 *       "password-confirmation": "testpass",
 *       "terms-of-service": true,
 *       "is-adult": false,
 *       "timezone": "Asia/Bangkok"
 *     }
 *   }
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 13.11.17
 */
@JsonApi(type = "users", policy = Policy.SERIALIZATION_ONLY)
class RegisterRequest : Resource() {

    /**
     * Email to register. This will be used in later login requests
     */
    @Json(name = "email")
    lateinit var email: String

    /**
     * Password for this email. API endpoints are HTTPS so we don't afraid of leakage
     */
    @Json(name = "password")
    lateinit var password: String

    /**
     * Confirmation for [password]
     */
    @Json(name = "password-confirmation")
    lateinit var confirmPassword: String

    /**
     * Currently active profile for this user. Usually used in PATCH requests
     * after user is already created
     */
    @Json(name = "active-profile")
    var activeProfile: String? = null

    /**
     * If this is unchecked server throws error that ToS are not accepted
     */
    @Json(name = "terms-of-service")
    var termsOfService: Boolean? = null

    /**
     * Whether to show non-SFW content
     */
    @Json(name = "is-adult")
    var isAdult: Boolean? = null

    /**
     * Timezone like 'Europe/Moscow', optional
     */
    @Json(name = "timezone")
    var timezone: String? = null
}

/**
 * Response from server on success after [RegisterRequest]
 *
 * ```
 * {
 *   "data": {
 *     "id": "5",
 *     "type": "users",
 *     "links": {
 *       "self": "http://www.example.com/v1/users/5"
 *     },
 *     "attributes": {
 *       "email": "test@example.com",
 *       "created-at": "2017-12-09T23:07:06.796Z",
 *       "updated-at": "2017-12-09T23:07:06.796Z",
 *       "is-over-18": false
 *     }
 *   }
 * }
 * ```
 *
 * @author Kanedias
 *
 * Created on 10.12.2017
 */
@JsonApi(type = "users", policy = Policy.DESERIALIZATION_ONLY)
class RegisterResponse : Dated() {

    /**
     * Email used when registering, see [RegisterRequest.email]
     */
    @Json(name = "email")
    lateinit var email: String

    /**
     * if checked - show non-SFW content
     */
    @Json(name = "is-adult")
    var isAdult: Boolean = false

    /**
     * Currently active profile for this user.
     * Can be null if user was just created and doesn't have any profile.
     */
    @Json(name = "active-profile")
    var activeProfile: String? = null

    /**
     * Available when requested with `include=profiles`
     */
    @Json(name = "profiles")
    var profiles = HasMany<OwnProfile>()
}

typealias User = RegisterResponse