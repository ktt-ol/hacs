package io.mainframe.hacs.cashbox

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

enum class UpdateType {
    // don't change the name, the endpoint expect exact those values
    Loss, Withdrawal, Donation, Deposit;
}

data class UpdateParam(val type: UpdateType, val amount: Int)

class UpdateCashboxTask(
    private val params: UpdateParam,
    auth: Auth,
    callback: (java.lang.Exception?, Boolean?, String?) -> Unit
) : BaseCashboxTask<Boolean>(auth, callback) {

    override fun buildRequests(): List<Request> {
        val cookie = getCookie() ?: throw java.lang.IllegalStateException("No cookie")
        val jsonBody = """{"update_type": "${params.type.name}", "amount": ${params.amount}}"""
            .toRequestBody("application/json".toMediaType())

        return listOf(
            Request.Builder()
                .url(CASHBOX_UPDATE_URL)
                .addHeader("Cookie", "sessionid=${cookie}")
                .post(jsonBody)
                .build()
        )
    }

    override fun handleSuccess(responses: List<RequestWithResponse>): Boolean {
        return true
    }
}
