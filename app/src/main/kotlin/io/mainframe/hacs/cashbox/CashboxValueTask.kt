package io.mainframe.hacs.cashbox

import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.pmw.tinylog.Logger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CashboxInfo(
    val value: String,
    val history: List<CashboxAction>,
    val requestedAt: Long = System.currentTimeMillis()
)

class CashboxValueTask(
    auth: Auth,
    callback: (java.lang.Exception?, CashboxInfo?, String?) -> Unit
) : BaseCashboxTask<CashboxInfo>(auth, callback) {

    override fun buildRequests(): List<Request> {
        val cookie = getCookie() ?: throw java.lang.IllegalStateException("No cookie")
        return listOf(
            Request.Builder().url(CASHBOX_STATUS_URL)
                .addHeader("Cookie", "sessionid=${cookie}")
                .build(),
            Request.Builder().url(CASHBOX_HISTORY_URL)
                .addHeader("Cookie", "sessionid=${cookie}")
                .build(),
        )
    }

    override fun handleSuccess(responses: List<RequestWithResponse>): CashboxInfo {
        var value: String? = null
        var history: List<CashboxAction>? = null
        responses.forEach { resp ->
            when (resp.request.url.toString()) {
                CASHBOX_STATUS_URL -> {
                    value = resp.bodyStr
                }
                CASHBOX_HISTORY_URL -> {
                    history = parseHistoryJson(resp.bodyStr)
                }
            }
        }

        return CashboxInfo(
            checkNotNull(value) { "Missing status value" },
            checkNotNull(history) { "Missing history value" }
        )
    }

    internal fun parseHistoryJson(jsonContent: String): List<CashboxAction> {
        val result = mutableListOf<CashboxAction>()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MMMM-dd HH:mm")

        val data = try {
            JSONArray(jsonContent)
        } catch (e: JSONException) {
            Logger.error("Can't parse json, because: {}.\nJson data:{}", e.message, jsonContent)
            throw e
        }
        for (i in 0 until data.length()) {
            val entry = data.getJSONObject(i)
            val time = Instant.ofEpochSecond(entry.getLong("timestamp"))
                .atZone(ZoneId.systemDefault())
            result.add(
                CashboxAction(
                    date = formatter.format(time),
                    name = entry.getString("username"),
                    amount = entry.getInt("amount")
                )
            )
        }

        return result
    }
}
