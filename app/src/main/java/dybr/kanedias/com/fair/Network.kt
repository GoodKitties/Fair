package dybr.kanedias.com.fair

import android.content.Context
import android.widget.Toast
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import dybr.kanedias.com.fair.entities.*
import dybr.kanedias.com.fair.misc.HttpApiException
import dybr.kanedias.com.fair.misc.HttpException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import moe.banana.jsonapi2.Document
import moe.banana.jsonapi2.ResourceAdapterFactory
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.net.HttpURLConnection.*


/**
 * @author Kanedias
 *
 * Created on 12.11.17
 */
object Network {

    private val MAIN_DYBR_API_ENDPOINT = "https://dybr-staging-api.herokuapp.com/v1"

    val IDENTITY_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/identity"
    val USERS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/users"
    val SESSIONS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/sessions"
    val CURRENT_IDENTITY_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/current_identity"
    val REGISTER_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/register"
    val LOGIN_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/login"
    val ENTRIES_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/entries"

    val MIME_JSON_API = MediaType.parse("application/vnd.api+json")

    private val jsonApiAdapter = ResourceAdapterFactory.builder()
            .add(Account::class.java)
            .add(LoginRequest::class.java)
            .add(RegisterRequest::class.java)
            .add(RegisterResponse::class.java)
            .build()

    private val jsonConverter = Moshi.Builder()
            .add(jsonApiAdapter)
            .build()

    lateinit var httpClient: OkHttpClient
    private lateinit var cookieJar: PersistentCookieJar

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

    fun register(regInfo: RegisterRequest): RegisterResponse {
        val reqAdapter = jsonConverter.adapter(RegisterRequest::class.java)
        val reqBody = RequestBody.create(MIME_JSON_API, reqAdapter.toJson(regInfo))
        val req = Request.Builder().post(reqBody).url(USERS_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            if (resp.code() == 422) { // Unprocessable entity
                // try to get error info
                val errAdapter = jsonConverter.adapter(Document::class.java)
                val errDoc = errAdapter.fromJson(resp.body()!!.source())!!
                throw HttpApiException(resp, errDoc.errors)
            }
            throw HttpException(resp)
        }

        val respAdapter = jsonConverter.adapter(RegisterResponse::class.java)
        return respAdapter.fromJson(resp.body()!!.source())!!
    }

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
        val adapter = jsonConverter.adapter(LoginRequest::class.java)
        val body = RequestBody.create(MIME_JSON_API, adapter.toJson(loginRequest))
        val req = Request.Builder().post(body).url(SESSIONS_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()

        // unauthorized error is returned when something is wrong with your input
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
        val adapter = jsonConverter.adapter(Identity::class.java)
        val identity = adapter.fromJson(identityObj)!!
        acc.apply {
            //name = identity.name
            //profile = identity
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
        val adapter = jsonConverter.adapter(EntryContainer::class.java)
        val container = adapter.fromJson(resp.body()!!.source())!!
        return container.entries
    }

    /**
     * Create diary entry on server.
     * @param entry entry to create. Should be filled with author, title, body content etc.
     * TODO: rewrite when actual API docs are available, add examples
     */
    fun createEntry(entry: DiaryEntry) {
        val adapter = jsonConverter.adapter(DiaryEntry::class.java)
        val body = RequestBody.create(MIME_JSON_API, adapter.toJson(entry))
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
            if (errMapping.containsKey(HTTP_OK)) {
                Toast.makeText(ctx, errMapping.getValue(HTTP_OK), Toast.LENGTH_SHORT).show()
            }
            return success.await()
        } catch (apiex: HttpApiException) { // API exchange error, most specific
            for (error in apiex.errors) {
                Toast.makeText(ctx, error.detail, Toast.LENGTH_SHORT).show()
            }
            onErr()
        } catch (httpex: HttpException) { // non-200 code, but couldn't deduce any API errors
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
        } catch (ioex: IOException) { // generic connection-level error, show as-is
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