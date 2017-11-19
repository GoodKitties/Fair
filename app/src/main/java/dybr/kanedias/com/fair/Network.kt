package dybr.kanedias.com.fair

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import dybr.kanedias.com.fair.entities.*
import okhttp3.*
import org.json.JSONObject
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
    val CURRENT_IDENTITY_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/current_identity"
    val REGISTER_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/register"
    val LOGIN_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/login"
    val ENTRIES_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/entries"

    val MIME_JSON = MediaType.parse("application/json")

    lateinit var httpClient: OkHttpClient
    lateinit var cookieJar: PersistentCookieJar

    fun init(ctx: Context) {
        cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(ctx))
        httpClient =  OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool())
                .cookieJar(cookieJar)
                .dispatcher(Dispatcher())
                .build()
    }

    /**
     * Check that cookies are not expired.
     * The relevant cookies are "express:sess" and "express:sess.sig"
     */
    fun cookiesInvalid(): Boolean = cookieJar.loadForRequest(HttpUrl.parse(MAIN_DYBR_API_ENDPOINT)!!).isEmpty()

    /**
     * Logs in with specified account.
     * This also clears previous and sets corresponding cookies.
     *
     * *This should be done in background thread*
     * @return true if auth was successful, false otherwise
     * @throws IOException on connection fail
     */
    fun login(acc: Account): Boolean {
        // clear present cookies, we're re-logging
        cookieJar.clear()
        if (acc === Auth.guest) {
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
        if (!resp.isSuccessful || Network.cookiesInvalid()) {
            return false
        }

        // without identity we won't know the name of the user
        if (!populateIdentity(acc)) {
            return false
        }

        return true
    }

    /**
     * Pull profile of current user. Http client should have relevant cookie at this point.
     * @return true if account was successfully populated with current profile and identity, false otherwise
     * @throws IOException on connection fail
     */
    fun populateIdentity(acc: Account): Boolean {
        val req = Request.Builder().url(CURRENT_IDENTITY_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful)
            return false

        // response is returned after execute call, body is not null
        val body = resp.body()!!.string()
        val identityObj = JSONObject(body).get("identity").toString() // stupid as hell
        val identity = Gson().fromJson(identityObj, Identity::class.java)
        acc.apply {
            name = identity.name
            profile = identity
        }
        return true
    }

    /**
     * Pull requested diary entries.
     * Examples of urls to pass to this function:
     * ```
     * https://dybr.ru/api/identity/<your-identity-name>/favorites
     * https://dybr.ru/api/entries/<someones-identity-name>
     * ```
     *
     * @param endpointUrl full url of endpoint to pull from
     */
    fun getEntries(endpointUrl: String): List<DiaryEntry> {
        val req = Request.Builder().url(endpointUrl).build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful)
            return emptyList()

        // response is returned after execute call, body is not null
        val body = resp.body()!!.string()
        val container = Gson().fromJson(body, EntryContainer::class.java)
        return container.entries
    }

    private class EntryContainer {
        lateinit var entries: List<DiaryEntry>
    }
}