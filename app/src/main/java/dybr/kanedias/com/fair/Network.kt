package dybr.kanedias.com.fair

import android.content.Context
import android.widget.Toast
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dybr.kanedias.com.fair.entities.*
import dybr.kanedias.com.fair.misc.Android
import dybr.kanedias.com.fair.misc.HttpApiException
import dybr.kanedias.com.fair.misc.HttpException
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import moe.banana.jsonapi2.Document
import moe.banana.jsonapi2.ObjectDocument
import moe.banana.jsonapi2.ResourceAdapterFactory
import moe.banana.jsonapi2.ResourceIdentifier
import okhttp3.*
import okio.BufferedSource
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

    private val USERS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/users"
    private val SESSIONS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/sessions"
    val CURRENT_IDENTITY_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/current_identity"

    private val MIME_JSON_API = MediaType.parse("application/vnd.api+json")

    private val jsonApiAdapter = ResourceAdapterFactory.builder()
            .add(LoginRequest::class.java)
            .add(LoginResponse::class.java)
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

    fun register(regInfo: RegisterRequest): RegisterResponse {
        val reqBody = RequestBody.create(MIME_JSON_API, toWrappedJson(regInfo))
        val req = Request.Builder().post(reqBody).url(USERS_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw extractErrors(resp)
        }

        return fromWrappedJson(resp.body()!!.source(), RegisterResponse::class.java)
    }

    /**
     * Tries to deduce errors from non-successful response.
     * @param resp http response that was not successful
     * @return [HttpApiException] if specific errors were found or generic [HttpException] for this response
     */
    private fun extractErrors(resp: Response) : HttpException {
        if (resp.code() == 422) { // Unprocessable entity
            // try to get error info
            val errAdapter = jsonConverter.adapter(Document::class.java)
            val errDoc = errAdapter.fromJson(resp.body()!!.source())!!
            return HttpApiException(resp, errDoc.errors)
        }
        return HttpException(resp)
    }

    /**
     * Logs in with specified account, i.e. creates session and obtains access token for this user
     *
     * *This should be done in background thread*
     *
     * @param
     * @return true if auth was successful, false otherwise
     * @throws IOException on connection fail
     */
    fun login(acc: Account) {
        if (acc === Auth.guest) {
            // it's guest, leave it as-is
            return
        }

        // clear present access token, we're re-logging
        acc.accessToken = null

        val loginRequest = LoginRequest().apply {
            email = acc.email
            password = acc.password
        }
        val body = RequestBody.create(MIME_JSON_API, toWrappedJson(loginRequest))
        val req = Request.Builder().post(body).url(SESSIONS_ENDPOINT).build()
        val resp = Network.httpClient.newCall(req).execute()

        // unprocessable entity error can be returned when something is wrong with your input
        if (!resp.isSuccessful) {
            throw extractErrors(resp)
        }

        // if response is successful we should have login response in body
        val respAdapter = jsonConverter.adapter(LoginResponse::class.java)
        val response = respAdapter.fromJson(resp.body()!!.source())!!
        acc.accessToken = response.accessToken
    }

    private fun <T: ResourceIdentifier> toWrappedJson(obj: T): String {
        val wrapped = ObjectDocument<T>().apply { set(obj) }
        val type = Types.newParameterizedType(Document::class.java, obj::class.java)
        val reqAdapter = jsonConverter.adapter<Document<T>>(type)
        return reqAdapter.toJson(wrapped)
    }

    private fun <T: ResourceIdentifier> fromWrappedJson(obj: BufferedSource, clazz: Class<T>): T {
        val type = Types.newParameterizedType(Document::class.java, clazz)
        val reqAdapter = jsonConverter.adapter<Document<T>>(type)
        val doc = reqAdapter.fromJson(obj)!!
        return doc.asObjectDocument().get()
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
     * Use this to handle typical errors in [Network]-invoked operations
     * @param ctx context from which to extract error strings
     * @param job main job to execute. The actual invocation happens in pooled thread. If all goes well no error mappings are needed
     * @param errMapping map containing correlation between HTTP return codes and messages
     * @param onErr job to execute if anything bad happens
     */
    fun <T> makeAsyncRequest(ctx: Context,
                                 job: () -> T,
                                 errMapping: Map<Int, Int> = emptyMap(),
                                 onErr: () -> Unit = {}): T? {
        var answer: T? = null

        launch(Android) {
            try {
                // execute main job that can throw
                val success = async(CommonPool) { job() }
                answer = success.await()

                // all went well, report if we should
                if (errMapping.containsKey(HTTP_OK)) {
                    Toast.makeText(ctx, errMapping.getValue(HTTP_OK), Toast.LENGTH_SHORT).show()
                }
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
        }

        return answer
    }

    private class EntryContainer {
        lateinit var entries: List<DiaryEntry>
    }
}