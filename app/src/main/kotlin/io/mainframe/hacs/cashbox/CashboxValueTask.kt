package io.mainframe.hacs.cashbox

import android.os.AsyncTask
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import org.pmw.tinylog.Logger
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CashboxAction(val date: String, val name: String, val amount: Int)

data class CashboxInfo(
    val value: String,
    val history: List<CashboxAction>,
    val requestedAt: Long = System.currentTimeMillis()
)

class CashboxValueTask(
    private val auth: Auth,
    private val callback: (TaskResult<CashboxInfo>) -> Unit
) : AsyncTask<Void, Void, Void>() {

    private var result: TaskResult<CashboxInfo>? = null

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Void?): Void? {
        try {
            val (currentCookie, cashboxStatus) = CashBoxRequester.withCookieAuth(auth) { cookieValue ->
                val data = CashBoxRequester.executeRequest(
                    Request.Builder().url(CASHBOX_STATUS_URL)
                        .addHeader("Cookie", "sessionid=${cookieValue}")
                        .build()
                )
                cookieValue to data
            }

            val history = parseHistoryJson(
                CashBoxRequester.executeRequest(
                    Request.Builder().url(CASHBOX_HISTORY_URL)
                        .addHeader("Cookie", "sessionid=${currentCookie}")
                        .build()
                )
            )
            result = TaskResult(null, CashboxInfo(cashboxStatus, history), currentCookie)
        } catch (e: Exception) {
            Logger.error(e, "Cashbox info error: ${e.message}")
            result = TaskResult(e, null, null)
        }


        return null
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Void?) {
        this.callback(checkNotNull(this.result) { "Missing result object!" })
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
