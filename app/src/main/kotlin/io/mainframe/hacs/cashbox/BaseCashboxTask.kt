package io.mainframe.hacs.cashbox

import android.os.AsyncTask
import okhttp3.*
import org.pmw.tinylog.Logger
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*

const val BASE_URL = "https://shop.mainframe.io"
const val LOGIN_URL = "$BASE_URL/login"
const val CASHBOX_STATUS_URL = "${BASE_URL}/cashbox/status"
const val CASHBOX_HISTORY_URL = "${BASE_URL}/cashbox/history"
const val CASHBOX_UPDATE_URL = "${BASE_URL}/cashbox/update"

data class Auth(val user: String, val password: String, val cookie: String?)

data class CashboxAction(val date: String, val name: String, val amount: Int)

open class EndpointException(msg: String, val httpBody: String, response: Response?) :
    IllegalStateException(msg) {
    val httpStatusCode: Int = response?.code ?: 0
    val httpStatusMsg: String = response?.message ?: ""

    override fun toString(): String {
        return "${super.message}\n${httpStatusCode}: ${httpStatusMsg}\n${httpBody}"
    }
}

class LoginFailed(msg: String, httpBody: String, response: Response?) :
    EndpointException(msg, httpBody, response)

class UnexpectedResponse(msg: String, httpBody: String, response: Response?) :
    EndpointException(msg, httpBody, response)

class RequestWithResponse(val request: Request, val bodyStr: String)

abstract class BaseCashboxTask<Result>(
    private val auth: Auth,
    private val callback: (java.lang.Exception?, Result?, String?) -> Unit
) : AsyncTask<Void, Void, Void>() {
    private var exception: Exception? = null
    private var result: Result? = null
    private var createdCookie: String? = null

    protected fun getCookie(): String? {
        return this.createdCookie ?: this.auth.cookie
    }

    abstract fun buildRequests(): List<Request>

    abstract fun handleSuccess(responses: List<RequestWithResponse>): Result

    fun login(user: String, password: String): String {
        val client = OkHttpClient.Builder()
            .build()

        val formBody = FormBody.Builder()
            .add("userid", user)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url(LOGIN_URL)
            .post(formBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 200) {
                throw LoginFailed(
                    "Unexpected code ${response.code}.",
                    response.body!!.string(),
                    response
                )
            }

            val setCookie = response.header("Set-Cookie")
                ?: throw LoginFailed("No cookie present", response.body!!.string(), response)

            val cookie = Cookie.Companion.parse(request.url, setCookie)
                ?: throw LoginFailed(
                    "Can't parse cookie: $setCookie",
                    response.body!!.string(),
                    response
                )

            Logger.debug("Login successful")
            return cookie.value
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        try {
            val client = OkHttpClient.Builder()
                .build()

            if (auth.cookie == null) {
                Logger.debug("No cookie present, do login.")
                this.createdCookie = login(auth.user, auth.password)
            }

            val successfulRequests = buildRequests().map { request ->
                handleRequest(client, request, false)
            }
            this.result = handleSuccess(successfulRequests)

        } catch (e: Exception) {
            Logger.error(e, "Cashbox error: ${e.message}")
            exception = e
        }

        return null
    }

    internal fun handleRequest(
        client: OkHttpClient,
        request: Request,
        isRetry: Boolean = false
    ): RequestWithResponse {
        client.newCall(request).execute().use { response ->
            val bodyStr = response.body!!.string()
            when (response.code) {
                200 -> {
                    Logger.debug("Got successfully cashbox data.")
                    return RequestWithResponse(request, bodyStr)
                }
                403 -> {
                    if (isRetry) {
                        throw UnexpectedResponse(
                            "Error after retry: ${response.code}",
                            bodyStr,
                            response
                        )
                    }

                    Logger.debug("Saved Cookie is invalid, do login.")
                    this.createdCookie = login(auth.user, auth.password)
                    return handleRequest(client, request, true)
                }
                else -> {
                    throw UnexpectedResponse(
                        "Unexpected code: ${response.code}",
                        bodyStr,
                        response
                    )
                }
            }
        }
    }

    override fun onPostExecute(result: Void?) {
        this.callback(this.exception, this.result, this.createdCookie)
    }

    private fun trustManagerForCertificates(inputStream: InputStream): Pair<Array<KeyManager>, Array<TrustManager>> {
        val certificateFactory = CertificateFactory.getInstance("X.509")
        val certificates = certificateFactory.generateCertificates(inputStream)
        require(!certificates.isEmpty()) { "expected non-empty set of trusted certificates" }

        // Put the certificates a key store.
        val password = "password".toCharArray() // Any password will work.
        val keyStore = newEmptyKeyStore(password)
        for ((index, certificate) in certificates.withIndex()) {
            val certificateAlias = index.toString()
            keyStore.setCertificateEntry(certificateAlias, certificate)
        }

        // Use it to build an X509 trust manager.
        val keyManagerFactory = KeyManagerFactory.getInstance(
            KeyManagerFactory.getDefaultAlgorithm()
        )
        keyManagerFactory.init(keyStore, password)
        val trustManagerFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        trustManagerFactory.init(keyStore)

        return Pair(keyManagerFactory.keyManagers, trustManagerFactory.trustManagers)
    }

    private fun newEmptyKeyStore(password: CharArray): KeyStore {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        val inputStream: InputStream? = null // By convention, 'null' creates an empty key store.
        keyStore.load(inputStream, password)
        return keyStore
    }
}
