package dybr.kanedias.com.fair

import android.content.Context
import android.widget.Toast
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.google.gson.Gson
import dybr.kanedias.com.fair.entities.*
import dybr.kanedias.com.fair.misc.HttpException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import okhttp3.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.net.HttpRetryException


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
    fun login(acc: Account) {
        // clear present cookies, we're re-logging
        cookieJar.clear()
        if (acc === Auth.guest) {
            // it's guest, leave it as-is
            return
        }

        val loginRequest = LoginRequest(
                email = acc.email,
                password = acc.password
        )
        val body = RequestBody.create(MIME_JSON, Gson().toJson(loginRequest))
        val req = Request.Builder().post(body).url(LOGIN_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()

        // unauthorized error is returned when smth is wrong with your input
        if (!resp.isSuccessful) {
            throw HttpException(resp)
        }

        // we should have the cookie now
        if (Network.cookiesInvalid()) {
            throw IllegalStateException("Couldn't get required cookie from the site!") // should not happen
        }

        // without identity we won't know the name of the user
        populateIdentity(acc)
    }

    /**
     * Pull profile of current user. Http client should have relevant cookie at this point.
     * @return true if account was successfully populated with current profile and identity, false otherwise
     * @throws IOException on connection fail
     */
    fun populateIdentity(acc: Account) {
        val req = Request.Builder().url(CURRENT_IDENTITY_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful)
            throw HttpException(resp)

        // response is returned after execute call, body is not null
        val body = resp.body()!!.string()
        val identityObj = JSONObject(body).get("identity").toString() // stupid as hell
        val identity = Gson().fromJson(identityObj, Identity::class.java)
        acc.apply {
            name = identity.name
            profile = identity
        }
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
        if (!resp.isSuccessful) {
            throw HttpException(resp)
        }

        // response is returned after execute call, body is not null
        val body = resp.body()!!.string()
        val container = Gson().fromJson(body, EntryContainer::class.java)
        return container.entries
    }

    /**
     * Create diary entry on server.
     * @param entry entry to create. Should be filled with author, title, body content etc.
     * TODO: rewrite when actual API docs are available, add examples
     */
    fun createEntry(entry: DiaryEntry) {
        val body = RequestBody.create(MIME_JSON, Gson().toJson(entry))
        val req = Request.Builder().post(body).url("something").build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw HttpException(resp)
        }
    }

    /**
     * Use this in `launch(Android)` to handle typical errors in [Network]-invoked operations
     * @param ctx context from which to extract error strings
     * @param job main job to execute. The actual invocation happens in pooled thread. If all goes well no error mappings are needed
     * @param errMapping map containing correlation between HTTP return codes and messages
     * @param onErr job to execute if anything bad happens
     */
    suspend fun <T> makeAsyncRequest(ctx: Context,
                                 job: () -> T,
                                 errMapping: Map<Int, Int> = emptyMap(),
                                 onErr: () -> Unit = {}): T? {
        try {
            // execute main job that can throw
            val success = async(CommonPool) { job() }

            // all went well, report if we should
            if (errMapping.containsKey(HttpURLConnection.HTTP_OK)) {
                Toast.makeText(ctx, errMapping.getValue(HttpURLConnection.HTTP_OK), Toast.LENGTH_SHORT).show()
            }
            return success.await()
        } catch (httpex: HttpException) { // non-200 code
            // something happened, see if we have appropriate map
            if (errMapping.containsKey(httpex.code)) {
                // we have, take value from map
                val key = ctx.getString(errMapping.getValue(httpex.code))
                Toast.makeText(ctx, key, Toast.LENGTH_SHORT).show()
            } else {
                // we haven't, show generic error
                val errorText = ctx.getString(R.string.unexpected_website_error)
                Toast.makeText(ctx, "$errorText: ${httpex.message}", Toast.LENGTH_SHORT).show()
            }
            onErr()
        } catch (ioex: IOException) {
            // generic connection error, show as-is
            val errorText = ctx.getString(R.string.error_connecting)
            Toast.makeText(ctx, "$errorText: ${ioex.localizedMessage}", Toast.LENGTH_SHORT).show()
            onErr()
        }

        return null
    }

    private class EntryContainer {
        lateinit var entries: List<DiaryEntry>
    }
}