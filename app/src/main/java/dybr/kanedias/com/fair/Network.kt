package dybr.kanedias.com.fair

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.MediaType
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * @author Kanedias
 *
 * Created on 12.11.17
 */
object Network {

    private val MAIN_DYBR_API_ENDPOINT = "https://hidden-shelf-73183.herokuapp.com/api"

    val IDENTITY_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/identity"
    val USER_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/user"
    val REGISTER_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/register"

    val MIME_JSON = MediaType.parse("application/json")

    val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool())
            .dispatcher(Dispatcher())
            .build()
}