package dybr.kanedias.com.fair

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import dybr.kanedias.com.fair.entities.Auth
import dybr.kanedias.com.fair.entities.db.Account
import dybr.kanedias.com.fair.entities.dto.LoginRequest
import okhttp3.*
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.io.IOException

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
    val LOGIN_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/login"

    val MIME_JSON = MediaType.parse("application/json")

    lateinit var httpClient: OkHttpClient
    lateinit var cookieJar: PersistentCookieJar

    fun init(ctx: Context) {
        cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(ctx))
        httpClient =  OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool())
                .cookieJar(cookieJar)
                .dispatcher(Dispatcher())
                .build()
    }

    fun cookiesNotExpired(): Boolean = !cookieJar.loadForRequest(HttpUrl.parse(MAIN_DYBR_API_ENDPOINT)!!).isEmpty()

    /**
     * Logs in with specified account and returns true if auth was successful, false otherwise.
     * This also clears previous and sets corresponding cookies.
     *
     * *This should be done in background thread*
     * @throws IOException on connection fail
     */
    fun login(acc: Account): Boolean {
        // clear present cookies, we're re-logging
        cookieJar.clear()
        if (Auth.user === Auth.guest) {
            // it's guest, leave it as-is
            return true
        }

        val loginRequest = LoginRequest(
                email = acc.email,
                password = acc.password
        )
        val body = RequestBody.create(MIME_JSON, Gson().toJson(loginRequest))
        val req = Request.Builder().post(body).url(LOGIN_ENDPOINT).build()

        val resp = Network.httpClient.newCall(req).execute()

        // unauthorized error is returned when smth is wrong with your input
        if (resp.networkResponse()?.code() == HttpURLConnection.HTTP_UNAUTHORIZED) {
            return false
        }

        // we should have the cookie now
        if (!resp.isSuccessful || resp.header("Set-Cookie") == null) {
            return false
        }

        return true
    }


}