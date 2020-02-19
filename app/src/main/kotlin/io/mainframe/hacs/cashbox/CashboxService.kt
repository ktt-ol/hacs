package io.mainframe.hacs.cashbox

import android.os.AsyncTask
import okhttp3.*
import okio.Buffer
import org.pmw.tinylog.Logger
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.*

const val cashboxUrl = "https://shop.mainframe.io/cashbox"
const val loginUrl = "https://shop.mainframe.io/"

data class Auth(val user: String, val password: String, val cookie: String?) {}

data class CashboxAction(val date: String, val name: String, val amount: String) {}

data class CashboxInfo(val value: String, val history: List<CashboxAction>, val requestedAt: Long = System.currentTimeMillis()) {}

open class EndpointException(msg: String, response: Response?) : IllegalStateException(msg) {
    val httpStatusCode: Int = response?.code ?: 0
    val httpStatusMsg: String = response?.message ?: ""
    val httpBody: String = response?.body?.string() ?: ""

    override fun toString(): String {
        return "${super.message}\n${httpStatusCode}: ${httpStatusMsg}\n${httpBody}"
    }
}

class LoginFailed(msg: String, response: Response?) : EndpointException(msg, response) {
}

class UnexpectedResponse(msg: String, response: Response?) : EndpointException(msg, response) {
}

class CashboxValueTask(private val auth: Auth, private val callback: (java.lang.Exception?, CashboxInfo?, String?) -> Unit) : AsyncTask<Void, Void, Void>() {
    private var exception: Exception? = null
    private var result: CashboxInfo? = null
    private var createdCookie: String? = null

    fun fake(): Void? {
        val input = """
<!DOCTYPE html>
<html lang="en">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title>KTT Shop System: Cashbox</title>
		<link type="text/css" rel="stylesheet" href="/css/bootstrap.css" />
		<link type="text/css" rel="stylesheet" href="/css/bootstrap-theme.css" />
		<link type="text/css" rel="stylesheet" href="/css/dataTables.bootstrap.css" />
		<link type="text/css" rel="stylesheet" href="/css/base.css" />
		<script type="text/javascript" src="/js/jquery.js"></script>
		<script type="text/javascript" src="/js/jquery.mousewheel.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.selection.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.navigate.js"></script>
		<script type="text/javascript" src="/js/jquery.flot.pie.js"></script>
		<script type="text/javascript" src="/js/jquery.dataTables.js"></script>
		<script type="text/javascript" src="/js/bootstrap.js"></script>
		<script type="text/javascript" src="/js/dataTables.bootstrap.js"></script>
		<script type="text/javascript" src="/js/code39.js"></script>
	</head>
	<body>
		<div class="navbar navbar-default navbar-fixed-top">
			<a class="navbar-brand" href="/">KTT Shop System</a>

<ul class="navbar-nav nav">
	<li class=""><a href="/">Home</a></li>
	<li class=" dropdown">
		<a href="#" class="dropdown-toggle" data-toggle="dropdown">Products
			<b class="caret"></b>
		</a>
		<ul class="dropdown-menu">
			<li>
				<a href="/products">List</a>
			</li>
			<li>
				<a href="/products/bestbefore">Best before dates</a>
			</li>
			<li>
				<a href="/products/inventory">Start inventory</a>
			</li>
		</ul>
	</li>

	<li class=""><a href="/suppliers">Suppliers</a></li>

	<li class=""><a href="/aliases">Aliases</a></li>
	<li class="active "><a href="/cashbox">Cashbox</a></li>
<!--
	<li class=" dropdown">
		<a href="#" id="statsmenu" class="dropdown-toggle" data-toggle="dropdown">Statistics <b class="caret"></b></a>
		<ul class="dropdown-menu">
			<li><a href="/stats">Information</a></li>
			<li><a href="/stats/stock">Graph: Stock</a></li>
			<li><a href="/stats/profit_per_day">Graph: Profit / Day</a></li>
			<li><a href="/stats/profit_per_weekday">Graph: Profit / Weekday</a></li>
			<li><a href="/stats/profit_per_product">Graph: Profit / Product</a></li>
		</ul>
	</li>
-->
	<li class="  dropdown">
		<a href="#" id="usersmenu" class="dropdown-toggle" data-toggle="dropdown">Users <b class="caret"></b></a>
		<ul class="dropdown-menu">
			<li><a href="/users">List</a></li>
			<li><a href="/users/import">Import</a></li>
			<li><a href="/users/import-pgp">Import PGP Keys</a></li>
		</ul>
	</li>
</ul>

<ul class="navbar-nav nav pull-right">
	<li id="usermenu" class="drop-down">
		<a href="#" id="usermenulink" role="button" class="dropdown-toggle" data-toggle="dropdown"><i class="icon-user"></i>&nbsp;Some User&nbsp;<b class="caret"></b></a>
		<ul class="dropdown-menu" role="menu" aria-labelledby="usermenulink">
			<li role="menuitem"><a tabindex="-1" href="/users/1">Personal Data</a></li>
			<li role="menuitem"><a tabindex="-1" href="/users/1/invoice">Invoice</a></li>
			<li role="menuitem"><a tabindex="-1" href="/users/1/stats">Statistics</a></li>
			<li role="menuitem" class="divider"></li>
			<li role="menuitem"><a tabindex="-1" href="/logout">Logout</a></li>
		</ul>
	</li>
</ul>


		</div>
		<div class="content">
			<legend>Current Cashbox Status</legend>

<div class="input-group">
	<input name"status" class="form-control" type="number" readonly="readonly" value="12.34"/>
	<div class="input-group-addon">€</div>
</div>

<legend>Update Cashbox</legend>
<form action="/cashbox/add" class="form-inline">
	<div class="form-group">
		<select name="type" size="1" class="form-control">
			<option value="loss">Loss (Money is missing from the cashbox)</option>
			<option value="withdrawal">Withdrawal (You removed money from the cashbox)</option>
			<option value="donation">Donation (Cashbox has more money, than there should be)</option>
			<option value="deposit">Deposit (You added money to the cashbox)</option>
		</select>
		<div class="input-group">
			<input name="amount" type="number" placeholder="Amount (in Cent)" class="form-control" />
			<div class="input-group-addon">Cent</div>
		</div>
		<button type="submit" class="form-control btn btn-primary"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button>
	</div>
</form>

<legend>History (last ten updates)</legend>
<table class="table table-bordered table-striped table-hover table-condensed table-nonfluid">
	<tr><td>Date &amp; Time</td><td>Name</td><td>Amount</td></tr>
	<tr><td>2020-02-11 02:27:14</td><td>Donation</td><td class="text-right">0.20 €</td></tr>
<tr><td>2020-02-10 21:24:37</td><td>Loss</td><td class="text-right">-1.40 €</td></tr>
<tr><td>2020-02-09 19:28:09</td><td>Loss</td><td class="text-right">-0.60 €</td></tr>
<tr><td>2020-02-08 07:21:45</td><td>Foo Bar</td><td class="text-right">-27.90 €</td></tr>
<tr><td>2020-02-08 07:19:45</td><td>Donation</td><td class="text-right">0.20 €</td></tr>
<tr><td>2020-02-06 14:02:42</td><td>Mr X</td><td class="text-right">0.00 €</td></tr>
<tr><td>2020-02-05 21:31:30</td><td>Foo Bar</td><td class="text-right">0.00 €</td></tr>
<tr><td>2020-02-04 19:37:41</td><td>Foo Bar</td><td class="text-right">-5.70 €</td></tr>
<tr><td>2020-02-04 19:37:13</td><td>Loss</td><td class="text-right">-0.40 €</td></tr>
<tr><td>2020-02-02 23:58:33</td><td>Mr X</td><td class="text-right">0.00 €</td></tr>
</table>

<legend>Details</legend>
<div class="form-inline">
	<div class="form-group">
		<input id="yearDetail" class="form-control" type="number" placeholder="Year"/>
		<input id="monthDetail" class="form-control" type="number" placeholder="Month"/>
		<button class="form-control btn btn-default" onclick="showDetails()"></span>Show Details</button>
	</div>
</div>

<script>
function showDetails() {
	var year = document.getElementById("yearDetail").value;
	var month = document.getElementById("monthDetail").value;
	location.href = location.pathname + "/details/" + year + "/" + month;
}
</script>

		</div>
	</body>
</html>

        """.trimIndent()
        this.result = this.parseHtml(input)
        return null
    }

    override fun doInBackground(vararg params: Void?): Void? {
//        return this.fake()

        try {
            val client = getCaCertSSLClient()

            val requestBuilder = Request.Builder().url(cashboxUrl)
            var cookie = auth.cookie
            if (cookie == null) {
                Logger.debug("No cookie present, do login.")
                cookie = login(auth.user, auth.password)
                this.createdCookie = cookie
            }

            requestBuilder.addHeader("Cookie", "session=${cookie}")

            client.newCall(requestBuilder.build()).execute().use { response ->
                val bodyStr = response.body!!.string()
                when (response.code) {
                    200 -> {
                        Logger.debug("Got successfully cashbox data.")
                        this.result = this.parseHtml(bodyStr)
                    }
                    401 -> {
                        Logger.debug("Saved Cookie is invalid, do login.")
                        cookie = login(auth.user, auth.password)
                        this.createdCookie = cookie

                        val retryRequest = Request.Builder()
                                .url(cashboxUrl)
                                .addHeader("Cookie", "session=${cookie}")
                                .build()
                        client.newCall(retryRequest).execute().use { responseRetry ->
                            val bodyStrRetry = responseRetry.body!!.string()
                            Logger.debug("Got successfully cashbox data in the second attempt.")
                            this.result = this.parseHtml(bodyStrRetry)
                        }
                    }
                    else -> {
                        throw UnexpectedResponse("Unexpected code: ${response.code}", response)
                    }
                }
            }
        } catch (e: Exception) {
            Logger.error(e, "Cashbox error: ${e.message}")
            exception = e
        }

        return null
    }

    internal fun parseHtml(htmlContent: String): CashboxInfo {
        // find <input name"status" class="form-control" type="number" readonly="readonly" value="12.34"/>
        // and extract the value
        val valueResult = """<input name"status".* value="([\d.]+)"/>""".toRegex().find(htmlContent)
                ?: throw UnexpectedResponse("Could not find the cashbox value", null)
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

    override fun onPostExecute(result: Void?) {
        this.callback(this.exception, this.result, this.createdCookie)
    }
}


private fun login(user: String, password: String): String {
    val client = getCaCertSSLClient()

    val formBody = FormBody.Builder()
            .add("user", user)
            .add("password", password)
            .build()

    val request = Request.Builder()
            .url(loginUrl)
            .post(formBody)
            .build()

    client.newCall(request).execute().use { response ->
        if (response.code != 200) {
            throw LoginFailed("Unexpected code ${response.code}.", response)
        }

        val setCookie = response.header("Set-Cookie")
                ?: throw LoginFailed("No cookie present", response)

        val cookie = Cookie.Companion.parse(request.url, setCookie)
                ?: throw LoginFailed("Can't parse cookie: ${setCookie}", response)

        Logger.debug("Login successful")
        return cookie.value
    }
}

private fun getCaCertSSLClient(): OkHttpClient {
    val CA_CERT_INTER_PEM = """
        -----BEGIN CERTIFICATE-----
        MIIG0jCCBLqgAwIBAgIBDjANBgkqhkiG9w0BAQsFADB5MRAwDgYDVQQKEwdSb290
        IENBMR4wHAYDVQQLExVodHRwOi8vd3d3LmNhY2VydC5vcmcxIjAgBgNVBAMTGUNB
        IENlcnQgU2lnbmluZyBBdXRob3JpdHkxITAfBgkqhkiG9w0BCQEWEnN1cHBvcnRA
        Y2FjZXJ0Lm9yZzAeFw0xMTA1MjMxNzQ4MDJaFw0yMTA1MjAxNzQ4MDJaMFQxFDAS
        BgNVBAoTC0NBY2VydCBJbmMuMR4wHAYDVQQLExVodHRwOi8vd3d3LkNBY2VydC5v
        cmcxHDAaBgNVBAMTE0NBY2VydCBDbGFzcyAzIFJvb3QwggIiMA0GCSqGSIb3DQEB
        AQUAA4ICDwAwggIKAoICAQCrSTURSHzSJn5TlM9Dqd0o10Iqi/OHeBlYfA+e2ol9
        4fvrcpANdKGWZKufoCSZc9riVXbHF3v1BKxGuMO+f2SNEGwk82GcwPKQ+lHm9WkB
        Y8MPVuJKQs/iRIwlKKjFeQl9RrmK8+nzNCkIReQcn8uUBByBqBSzmGXEQ+xOgo0J
        0b2qW42S0OzekMV/CsLj6+YxWl50PpczWejDAz1gM7/30W9HxM3uYoNSbi4ImqTZ
        FRiRpoWSR7CuSOtttyHshRpocjWr//AQXcD0lKdq1TuSfkyQBX6TwSyLpI5idBVx
        bgtxA+qvFTia1NIFcm+M+SvrWnIl+TlG43IbPgTDZCciECqKT1inA62+tC4T7V2q
        SNfVfdQqe1z6RgRQ5MwOQluM7dvyz/yWk+DbETZUYjQ4jwxgmzuXVjit89Jbi6Bb
        6k6WuHzX1aCGcEDTkSm3ojyt9Yy7zxqSiuQ0e8DYbF/pCsLDpyCaWt8sXVJcukfV
        m+8kKHA4IC/VfynAskEDaJLM4JzMl0tF7zoQCqtwOpiVcK01seqFK6QcgCExqa5g
        eoAmSAC4AcCTY1UikTxW56/bOiXzjzFU6iaLgVn5odFTEcV7nQP2dBHgbbEsPyyG
        kZlxmqZ3izRg0RS0LKydr4wQ05/EavhvE/xzWfdmQnQeiuP43NJvmJzLR5iVQAX7
        6QIDAQABo4IBiDCCAYQwHQYDVR0OBBYEFHWocWBMiBPweNmJd7VtxYnfvLF6MA8G
        A1UdEwEB/wQFMAMBAf8wXQYIKwYBBQUHAQEEUTBPMCMGCCsGAQUFBzABhhdodHRw
        Oi8vb2NzcC5DQWNlcnQub3JnLzAoBggrBgEFBQcwAoYcaHR0cDovL3d3dy5DQWNl
        cnQub3JnL2NhLmNydDBKBgNVHSAEQzBBMD8GCCsGAQQBgZBKMDMwMQYIKwYBBQUH
        AgEWJWh0dHA6Ly93d3cuQ0FjZXJ0Lm9yZy9pbmRleC5waHA/aWQ9MTAwNAYJYIZI
        AYb4QgEIBCcWJWh0dHA6Ly93d3cuQ0FjZXJ0Lm9yZy9pbmRleC5waHA/aWQ9MTAw
        UAYJYIZIAYb4QgENBEMWQVRvIGdldCB5b3VyIG93biBjZXJ0aWZpY2F0ZSBmb3Ig
        RlJFRSwgZ28gdG8gaHR0cDovL3d3dy5DQWNlcnQub3JnMB8GA1UdIwQYMBaAFBa1
        MhvUx/Pg5o7zvdKwOu6yORjRMA0GCSqGSIb3DQEBCwUAA4ICAQBakBbQNiNWZJWJ
        vI+spCDJJoqp81TkQBg/SstDxpt2CebKVKeMlAuSaNZZuxeXe2nqrdRM4SlbKBWP
        3Rn0lVknlxjbjwm5fXh6yLBCVrXq616xJtCXE74FHIbhNAUVsQa92jzQE2OEbTWU
        0D6Zghih+j+cN0eFiuDuc3iC1GuZMb/Zw21AXbkVxzZ4ipaL0YQgsSt1P22ipb69
        6OLkrURctgY2cHS4pI62VpRgkwJ/Lw2n+C9vtukozMhrlPSTA0OhNEGiGp2hRpWa
        hiG+HGcIYfAV9v7og3dO9TnS0XDbbk1RqXPpc/DtrJWzmZN0O4KIx0OtLJJWG9zp
        9JrJyO6USIFYgar0U8HHHoTccth+8vJirz7Aw4DlCujo27OoIksg3OzgX/DkvWYl
        0J8EMlXoH0iTv3qcroQItOUFsgilbjRba86Q5kLhnCxjdW2CbbNSp8vlZn0uFxd8
        spxQcXs0CIn19uvcQIo4Z4uQ+00Lg9xI9YFV9S2MbSanlNUlvbB4UvHkel0p6bGt
        Amp1dJBSkZOFm0Z6ek+G7w7R1aTifjGJrdw032O+VIKwCgu8DdskR0w0B68ydZn0
        ATnMnr5ExvcWkZBtCgQa2NvSKrcQnlaqo9icEF4XevI/VTezlb1LjYMWHVd5R6C2
        p4wTyVBIM8hjrLcKiChF43GRJtne7w==
        -----END CERTIFICATE-----
""".trimIndent()

    val CA_CERT_PEM = """
        -----BEGIN CERTIFICATE-----
        MIIG7jCCBNagAwIBAgIBDzANBgkqhkiG9w0BAQsFADB5MRAwDgYDVQQKEwdSb290
        IENBMR4wHAYDVQQLExVodHRwOi8vd3d3LmNhY2VydC5vcmcxIjAgBgNVBAMTGUNB
        IENlcnQgU2lnbmluZyBBdXRob3JpdHkxITAfBgkqhkiG9w0BCQEWEnN1cHBvcnRA
        Y2FjZXJ0Lm9yZzAeFw0wMzAzMzAxMjI5NDlaFw0zMzAzMjkxMjI5NDlaMHkxEDAO
        BgNVBAoTB1Jvb3QgQ0ExHjAcBgNVBAsTFWh0dHA6Ly93d3cuY2FjZXJ0Lm9yZzEi
        MCAGA1UEAxMZQ0EgQ2VydCBTaWduaW5nIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJ
        ARYSc3VwcG9ydEBjYWNlcnQub3JnMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC
        CgKCAgEAziLA4kZ97DYoB1CW8qAzQIxL8TtmPzHlawI229Z89vGIj053NgVBlfkJ
        8BLPRoZzYLdufujAWGSuzbCtRRcMY/pnCujW0r8+55jE8Ez64AO7NV1sId6eINm6
        zWYyN3L69wj1x81YyY7nDl7qPv4coRQKFWyGhFtkZip6qUtTefWIonvuLwphK42y
        fk1WpRPs6tqSnqxEQR5YYGUFZvjARL3LlPdCfgv3ZWiYUQXw8wWRBB0bF4LsyFe7
        w2t6iPGwcswlWyCR7BYCEo8y6RcYSNDHBS4CMEK4JZwFaz+qOqfrU0j36NK2B5jc
        G8Y0f3/JHIJ6BVgrCFvzOKKrF11myZjXnhCLotLddJr3cQxyYN/Nb5gznZY0dj4k
        epKwDpUeb+agRThHqtdB7Uq3EvbXG4OKDy7YCbZZ16oE/9KTfWgu3YtLq1i6L43q
        laegw1SJpfvbi1EinbLDvhG+LJGGi5Z4rSDTii8aP8bQUWWHIbEZAWV/RRyH9XzQ
        QUxPKZgh/TMfdQwEUfoZd9vUFBzugcMd9Zi3aQaRIt0AUMyBMawSB3s42mhb5ivU
        fslfrejrckzzAeVLIL+aplfKkQABi6F1ITe1Yw1nPkZPcCBnzsXWWdsC4PDSy826
        YreQQejdIOQpvGQpQsgi3Hia/0PsmBsJUUtaWsJx8cTLc6nloQsCAwEAAaOCAX8w
        ggF7MB0GA1UdDgQWBBQWtTIb1Mfz4OaO873SsDrusjkY0TAPBgNVHRMBAf8EBTAD
        AQH/MDQGCWCGSAGG+EIBCAQnFiVodHRwOi8vd3d3LmNhY2VydC5vcmcvaW5kZXgu
        cGhwP2lkPTEwMFYGCWCGSAGG+EIBDQRJFkdUbyBnZXQgeW91ciBvd24gY2VydGlm
        aWNhdGUgZm9yIEZSRUUgaGVhZCBvdmVyIHRvIGh0dHA6Ly93d3cuY2FjZXJ0Lm9y
        ZzAxBgNVHR8EKjAoMCagJKAihiBodHRwOi8vY3JsLmNhY2VydC5vcmcvcmV2b2tl
        LmNybDAzBglghkgBhvhCAQQEJhYkVVJJOmh0dHA6Ly9jcmwuY2FjZXJ0Lm9yZy9y
        ZXZva2UuY3JsMDIGCCsGAQUFBwEBBCYwJDAiBggrBgEFBQcwAYYWaHR0cDovL29j
        c3AuY2FjZXJ0Lm9yZzAfBgNVHSMEGDAWgBQWtTIb1Mfz4OaO873SsDrusjkY0TAN
        BgkqhkiG9w0BAQsFAAOCAgEAR5zXs6IX01JTt7Rq3b+bNRUhbO9vGBMggczo7R0q
        Ih1kdhS6WzcrDoO6PkpuRg0L3qM7YQB6pw2V+ubzF7xl4C0HWltfzPTbzAHdJtja
        JQw7QaBlmAYpN2CLB6Jeg8q/1Xpgdw/+IP1GRwdg7xUpReUA482l4MH1kf0W0ad9
        4SuIfNWQHcdLApmno/SUh1bpZyeWrMnlhkGNDKMxCCQXQ360TwFHc8dfEAaq5ry6
        cZzm1oetrkSviE2qofxvv1VFiQ+9TX3/zkECCsUB/EjPM0lxFBmu9T5Ih+Eqns9i
        vmrEIQDv9tNyJHuLsDNqbUBal7OoiPZnXk9LH+qb+pLf1ofv5noy5vX2a5OKebHe
        +0Ex/A7e+G/HuOjVNqhZ9j5Nispfq9zNyOHGWD8ofj8DHwB50L1Xh5H+EbIoga/h
        JCQnRtxWkHP699T1JpLFYwapgplivF4TFv4fqp0nHTKC1x9gGrIgvuYJl1txIKmx
        XdfJzgscMzqpabhtHOMXOiwQBpWzyJkofF/w55e0LttZDBkEsilV/vW0CJsPs3eN
        aQF+iMWscGOkgLFlWsAS3HwyiYLNJo26aqyWPaIdc8E4ck7Sk08WrFrHIK3EHr4n
        1FZwmLpFAvucKqgl0hr+2jypyh5puA3KksHF3CsUzjMUvzxMhykh9zrMxQAHLBVr
        Gwc=
        -----END CERTIFICATE-----
""".trimIndent()

    val certs = Buffer()
            .writeUtf8(CA_CERT_PEM)
            .writeUtf8("\n")
            .writeUtf8(CA_CERT_INTER_PEM)
            .inputStream()

    val (keyManagers, trustManagers) = trustManagerForCertificates(certs)
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagers, trustManagers, null)
    val sslSocketFactory = sslContext.socketFactory

    return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManagers[0] as X509TrustManager)
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