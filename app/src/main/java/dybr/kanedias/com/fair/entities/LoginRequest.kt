package dybr.kanedias.com.fair.entities

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
data class LoginRequest(
        val email: String,
        val password: String
)