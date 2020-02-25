package io.mainframe.hacs.cashbox

import okhttp3.Request

data class CashboxInfo(val value: String, val history: List<CashboxAction>, val requestedAt: Long = System.currentTimeMillis())

class CashboxValueTask(auth: Auth, callback: (java.lang.Exception?, CashboxInfo?, String?) -> Unit) : BaseCashboxTask<CashboxInfo>(auth, callback) {

    override fun buildRequest(): Request {
        val cookie = getCookie() ?: throw java.lang.IllegalStateException("No cookie")
        return Request.Builder().url(CASHBOX_URL)
                .addHeader("Cookie", "session=${cookie}")
                .build()
    }

    override fun handleSuccess(bodyStr: String): CashboxInfo {
        return this.parseHtml(bodyStr)
    }

    internal fun parseHtml(htmlContent: String): CashboxInfo {
        // find <input name"status" class="form-control" type="number" readonly="readonly" value="12.34"/>
        // and extract the value
        val valueResult = """<input name"status".* value="([-\d.]+)"/>""".toRegex().find(htmlContent)
                ?: throw UnexpectedResponse("Could not find the cashbox value", "", null)
        val value = valueResult.groupValues[1]

        // find <tr><td>Date &amp; Time</td><td>Name</td><td>Amount</td></tr>
        //   or <tr><td>2020-02-11 02:27:14</td><td>Donation</td><td class="text-right">0.20 €</td></tr>
        val actionResult = """<tr><td>([^<]+)</td><td>([^<]+)</td><td.*>([^<]+)</td></tr>""".toRegex().findAll(htmlContent)
        var history = actionResult.map {
            CashboxAction(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }.toList()

        // remeove the first line
        if (history.isNotEmpty()) {
            history = history.subList(1, history.size - 1)
        }

        return CashboxInfo(value, history)
    }
}
