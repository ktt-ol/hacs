package io.mainframe.hacs.cashbox

import android.os.AsyncTask
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.pmw.tinylog.Logger

enum class UpdateType {
    // don't change the name, the endpoint expect exact those values
    Loss, Withdrawal, Donation, Deposit;
}

data class UpdateParam(val type: UpdateType, val amount: Int)

class UpdateCashboxTask(
    private val params: UpdateParam,
    private val auth: Auth,
    private val callback: (TaskResult<Unit>) -> Unit
) : AsyncTask<Void, Void, Void>() {

    private var result: TaskResult<Unit>? = null

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg _params: Void?): Void? {
        val jsonBody = """{"update_type": "${params.type.name}", "amount": ${params.amount}}"""
            .toRequestBody("application/json".toMediaType())

        try {
            CashBoxRequester.withCookieAuth(auth) { cookieValue ->
                CashBoxRequester.executeRequest(
                    Request.Builder()
                        .url(CASHBOX_UPDATE_URL)
                        .addHeader("Cookie", "sessionid=${cookieValue}")
                        .post(jsonBody)
                        .build()
                )

                result = TaskResult(null, null, cookieValue)
            }
        } catch (e: Exception) {
            Logger.error(e, "Cashbox update error: ${e.message}")
            result = TaskResult(e, null, null)
        }
        return null
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Void?) {
        this.callback(checkNotNull(this.result) { "Missing result object!" })
    }
}
