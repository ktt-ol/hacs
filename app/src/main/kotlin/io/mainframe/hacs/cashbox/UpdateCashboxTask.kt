package io.mainframe.hacs.cashbox

import okhttp3.Request

enum class UpdateType {
    // don't change the name, the endpoint expect exact those values
    loss, withdrawal, donation, deposit;
}

data class UpdateParam(val type: UpdateType, val amount: Int)

class UpdateCashboxTask(private val params: UpdateParam, auth: Auth, callback: (java.lang.Exception?, Boolean?, String?) -> Unit) : BaseCashboxTask<Boolean>(auth, callback) {

    override fun buildRequest(): Request {
        val cookie = getCookie() ?: throw java.lang.IllegalStateException("No cookie")
        return Request.Builder().url("${CASHBOX_URL}/add?type=${this.params.type}&amount=${this.params.amount}")
                .addHeader("Cookie", "session=${cookie}")
                .build()
    }

    override fun handleSuccess(bodyStr: String): Boolean {
        return bodyStr.contains("""<div id="add-successful" class="alert alert-success" style="display: block;">""")
    }
}