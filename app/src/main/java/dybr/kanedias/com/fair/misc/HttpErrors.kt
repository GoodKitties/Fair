package dybr.kanedias.com.fair.misc

import moe.banana.jsonapi2.Error
import okhttp3.Response
import java.io.IOException

/**
 * Common http exception to work with when answer is not `200 OK`
 *
 * @author Kanedias
 *
 * Created on 29.11.17
 */
open class HttpException(val code: Int, message: String, val body: String) : IOException(message) {

    constructor(resp: Response): this(resp.code(), resp.message(), resp.body()!!.string())

    // we always have message in http exception via primary constructor
    override val message: String
        get() = super.message!!
}

/**
 * JSON-API exception containing error list
 *
 *  @author Kanedias
 *
 * Created on 10.12.17
 */
class HttpApiException(resp: Response, val errors: List<Error>) : HttpException(resp)