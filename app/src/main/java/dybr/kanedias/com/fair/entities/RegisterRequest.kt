package dybr.kanedias.com.fair.entities

/**
 * Register call request body.
 * All fields are necessary.
 * Example:
 * ```
 *  {
 *      "name":"Kanedias",
 *      "email":"sample@example.ru",
 *      "uri":"Kanedias",
 *      "password":"sample",
 *      "confirmPassword":"sample",
 *      "title":"House of Maker"
 *  }
 * ```
 *
 * @author Kanedias
 *
 * Created on 13.11.17
 */
data class RegisterRequest(
        val name: String,
        val email: String,
        val uri: String,
        val password: String,
        val confirmPassword: String,
        val title: String
)