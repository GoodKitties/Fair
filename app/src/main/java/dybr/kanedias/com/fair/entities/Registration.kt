package dybr.kanedias.com.fair.entities

import com.squareup.moshi.Json
import moe.banana.jsonapi2.JsonApi
import moe.banana.jsonapi2.Policy
import moe.banana.jsonapi2.Resource
import java.util.*

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
 *       "terms-of-service": true
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
    @Json(name = "email")
    lateinit var email: String

    @Json(name = "password")
    lateinit var password: String

    @Json(name = "confirm-password")
    lateinit var confirmPassword: String

    @Json(name = "terms-of-service")
    var termsOfService: Boolean = true
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
class RegisterResponse : Resource() {
    @Json(name = "email")
    lateinit var email: String

    @Json(name = "created-at")
    lateinit var createdAt: Date

    @Json(name = "updated-at")
    lateinit var updatedAt: Date

    @Json(name = "is-over-18")
    var isOver18: Boolean = false
}