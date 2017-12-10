package dybr.kanedias.com.fair.entities

import moe.banana.jsonapi2.JsonApi
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
@JsonApi(type = "sessions")
class LoginRequest(val action: String = "login", val email: String, val password: String): Resource()