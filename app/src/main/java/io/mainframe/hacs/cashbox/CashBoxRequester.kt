package io.mainframe.hacs.cashbox

import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.pmw.tinylog.Logger

const val BASE_URL = "https://shop.mainframe.io"
const val LOGIN_URL = "$BASE_URL/login"
const val CASHBOX_STATUS_URL = "${BASE_URL}/cashbox/status"
const val CASHBOX_HISTORY_URL = "${BASE_URL}/cashbox/history"
const val CASHBOX_UPDATE_URL = "${BASE_URL}/cashbox/update"

data class Auth(val user: String, val password: String, val cookie: String?)

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

// for http 403
class ForbiddenException(msg: String, httpBody: String, response: Response?) :
    EndpointException(msg, httpBody, response)

data class TaskResult<T>(
    val exception: Exception?,
    val result: T?,
    val createdCookie: String?,
)

object CashBoxRequester {

    fun <T> withCookieAuth(savedAuth: Auth, block: (cookieValue: String) -> T): T {
        if (savedAuth.cookie != null) {
            try {
                return block.invoke(savedAuth.cookie)
            } catch (e: ForbiddenException) {
                Logger.info("Cookie rejected, login again.")
            }
        }

        val cookieValue = login(savedAuth.user, savedAuth.password)
        return block.invoke(cookieValue)
    }

    /** returns the cookie value */
    private fun login(user: String, password: String): String {
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

    fun executeRequest(request: Request): String {
        OkHttpClient.Builder().build()
            .newCall(request).execute().use { response ->
                val bodyStr = response.body!!.string()
                when (response.code) {
                    200 -> {
                        Logger.debug("Got successfully cashbox data.")
                        return bodyStr
                    }

                    403 -> {
                        throw ForbiddenException("Cookie was rejected with 403", bodyStr, response)
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

}
