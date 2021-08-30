package io.mainframe.hacs.cashbox

import android.os.AsyncTask
import okhttp3.*
import okio.Buffer
import org.pmw.tinylog.Logger
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*

const val BASE_URL = "https://shop.mainframe.io"
//const val BASE_URL = "https://192.168.42.182"
const val LOGIN_URL = BASE_URL
const val CASHBOX_URL = "${BASE_URL}/cashbox"

data class Auth(val user: String, val password: String, val cookie: String?)

data class CashboxAction(val date: String, val name: String, val amount: String)

open class EndpointException(msg: String, val httpBody: String, response: Response?) : IllegalStateException(msg) {
    val httpStatusCode: Int = response?.code ?: 0
    val httpStatusMsg: String = response?.message ?: ""

    override fun toString(): String {
        return "${super.message}\n${httpStatusCode}: ${httpStatusMsg}\n${httpBody}"
    }
}

class LoginFailed(msg: String, httpBody: String, response: Response?) : EndpointException(msg, httpBody, response)
class UnexpectedResponse(msg: String, httpBody: String, response: Response?) : EndpointException(msg, httpBody, response)

abstract class BaseCashboxTask<Result>(private val auth: Auth, private val callback: (java.lang.Exception?, Result?, String?) -> Unit) : AsyncTask<Void, Void, Void>() {
    private var exception: Exception? = null
    private var result: Result? = null
    private var createdCookie: String? = null

    protected fun getCookie(): String? {
        return this.createdCookie ?: this.auth.cookie
    }

    abstract fun buildRequest(): Request

    abstract fun handleSuccess(bodyStr: String): Result

    private fun login(user: String, password: String): String {
        val client = getCaCertSSLClient()

        val formBody = FormBody.Builder()
                .add("user", user)
                .add("password", password)
                .build()

        val request = Request.Builder()
                .url(LOGIN_URL)
                .post(formBody)
                .build()

        client.newCall(request).execute().use { response ->
            if (response.code != 200) {
                throw LoginFailed("Unexpected code ${response.code}.", response.body!!.string(), response)
            }

            val setCookie = response.header("Set-Cookie")
                    ?: throw LoginFailed("No cookie present", response.body!!.string(), response)

            val cookie = Cookie.Companion.parse(request.url, setCookie)
                    ?: throw LoginFailed("Can't parse cookie: ${setCookie}", response.body!!.string(), response)

            Logger.debug("Login successful")
            return cookie.value
        }
    }

    override fun doInBackground(vararg params: Void?): Void? {
        try {
            val client = getCaCertSSLClient()

            var cookie = auth.cookie
            if (cookie == null) {
                Logger.debug("No cookie present, do login.")
                cookie = login(auth.user, auth.password)
                this.createdCookie = cookie
            }

            client.newCall(buildRequest()).execute().use { response ->
                val bodyStr = response.body!!.string()
                when (response.code) {
                    200 -> {
                        Logger.debug("Got successfully cashbox data.")
                        this.result = handleSuccess(bodyStr)
                    }
                    403 -> {
                        Logger.debug("Saved Cookie is invalid, do login.")
                        cookie = login(auth.user, auth.password)
                        this.createdCookie = cookie

                        client.newCall(buildRequest()).execute().use { responseRetry ->
                            val bodyStrRetry = responseRetry.body!!.string()
                            Logger.debug("Got successfully cashbox data in the second attempt.")
                            this.result = handleSuccess(bodyStrRetry)
                        }
                    }
                    else -> {
                        throw UnexpectedResponse("Unexpected code: ${response.code}", bodyStr, response)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(e, "Cashbox error: ${e.message}")
            exception = e
        }

        return null
    }

    override fun onPostExecute(result: Void?) {
        this.callback(this.exception, this.result, this.createdCookie)
    }

    private fun getCaCertSSLClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .build()
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
