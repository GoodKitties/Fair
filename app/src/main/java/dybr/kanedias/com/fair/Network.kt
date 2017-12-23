package dybr.kanedias.com.fair

import android.content.Context
import android.widget.Toast
import com.squareup.moshi.Moshi
import com.squareup.moshi.Rfc3339DateJsonAdapter
import com.squareup.moshi.Types
import dybr.kanedias.com.fair.entities.*
import dybr.kanedias.com.fair.misc.HttpApiException
import dybr.kanedias.com.fair.misc.HttpException
import moe.banana.jsonapi2.Document
import moe.banana.jsonapi2.ObjectDocument
import moe.banana.jsonapi2.ResourceAdapterFactory
import moe.banana.jsonapi2.ResourceIdentifier
import okhttp3.*
import okio.BufferedSource
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.io.IOException
import java.util.*


/**
 * @author Kanedias
 *
 * Created on 12.11.17
 */
object Network {

    private val MAIN_DYBR_API_ENDPOINT = "https://dybr-staging-api.herokuapp.com/v1"

    private val USERS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/users"
    private val SESSIONS_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/sessions"
    private val CURRENT_PROFILE_ENDPOINT = "$MAIN_DYBR_API_ENDPOINT/own-profiles"

    private val MIME_JSON_API = MediaType.parse("application/vnd.api+json")

    private val jsonApiAdapter = ResourceAdapterFactory.builder()
            .add(LoginRequest::class.java)
            .add(LoginResponse::class.java)
            .add(RegisterRequest::class.java)
            .add(RegisterResponse::class.java)
            .build()

    private val jsonConverter = Moshi.Builder()
            .add(jsonApiAdapter)
            .add(Date::class.java, Rfc3339DateJsonAdapter())
            .build()

    /**
     * OkHttp [Interceptor] for populating authorization header.
     *
     * Authorizes all requests if we have access token for account (i.e. logged in).
     */
    private val authorizer = Interceptor { chain ->
        val origRequest = chain.request()
        val alreadyHasAuth = origRequest.headers().names().contains("Authorization")

        // skip if we already have auth header in request
        if (Auth.user.accessToken == null || alreadyHasAuth)
            return@Interceptor chain.proceed(origRequest)

        val authorisedReq  = origRequest.newBuilder()
                .header("Authorization", "Bearer: ${Auth.user.accessToken}")
                .build()

        return@Interceptor chain.proceed(authorisedReq)
    }

    private lateinit var httpClient: OkHttpClient

    fun init(ctx: Context) {
        httpClient =  OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectionPool(ConnectionPool())
                .dispatcher(Dispatcher())
                .addInterceptor(authorizer)
                .build()
    }

    fun register(regInfo: RegisterRequest): RegisterResponse {
        val reqBody = RequestBody.create(MIME_JSON_API, toWrappedJson(regInfo))
        val req = Request.Builder().post(reqBody).url(USERS_ENDPOINT).build()
        val resp = httpClient.newCall(req).execute()
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
     * Logs in with specified account, i.e. creates session and obtains access token for this user.
     * After this call succeeds you may be certain that [httpClient] sends requests with correct
     * "Authorization" header.
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
        val resp = httpClient.newCall(req).execute()

        // unprocessable entity error can be returned when something is wrong with your input
        if (!resp.isSuccessful) {
            throw extractErrors(resp)
        }

        // if response is successful we should have login response in body
        val response = fromWrappedJson(resp.body()!!.source(), LoginResponse::class.java)

        // memorize the access token and use it in next requests
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
     * Pull profile of current user. Account should have relevant access token by this point.
     * @return true if account was successfully populated with current profile and identity, false otherwise
     * @throws IOException on connection fail
     */
    fun populateIdentity(acc: Account) {
        val req = Request.Builder().url(CURRENT_PROFILE_ENDPOINT).build()
        val resp = httpClient.newCall(req).execute()
        if (!resp.isSuccessful)
            throw HttpException(resp)

        // response is returned after execute call, body is not null
        val profile = fromWrappedJson(resp.body()!!.source(), OwnProfile::class.java)
        acc.lastProfile = profile.id
    }

    /**
     * Load all profiles
     */
    fun loadProfiles(acc: Account) {

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
        val resp = httpClient.newCall(req).execute()
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
        val resp = httpClient.newCall(req).execute()
        if (!resp.isSuccessful) {
            throw HttpException(resp)
        }
    }

    /**
     * Handle typical network-related problems.
     * * Handles API-level problems - answers with errors from API
     * * Handles HTTP-level problems - answers with strings from [errMapping]
     * * Handles conection IO-level problems
     * @param ctx context to get error string from
     * @param errMapping mapping of ids like `HttpUrlConnection.HTTP_NOT_FOUND -> R.string.not_found`
     */
    fun reportErrors(ctx: Context, ex: Exception, errMapping: Map<Int, Int> = emptyMap()) = when (ex) {
        // API exchange error, most specific
        is HttpApiException -> for (error in ex.errors) {
            Toast.makeText(ctx, error.detail, Toast.LENGTH_SHORT).show()
        }

        // non-200 code, but couldn't deduce any API errors
        is HttpException -> {
            if (errMapping.containsKey(ex.code)) {
                // we have, take value from map
                val key = ctx.getString(errMapping.getValue(ex.code))
                Toast.makeText(ctx, key, Toast.LENGTH_SHORT).show()
            } else {
                // we haven't, show generic error
                val errorText = ctx.getString(R.string.unexpected_website_error)
                Toast.makeText(ctx, "$errorText: ${ex.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // generic connection-level error, show as-is
        is IOException -> {
            val errorText = ctx.getString(R.string.error_connecting)
            Toast.makeText(ctx, "$errorText: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
        }

        else -> throw ex
    }

    private class EntryContainer {
        lateinit var entries: List<DiaryEntry>
    }
}