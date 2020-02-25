package io.mainframe.hacs.cashbox

import android.annotation.SuppressLint
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*


private class UnsafeTrustManager() : X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return arrayOf()
    }
}

/**
 * Returns a client that accept any ssl cert. Helpful for internal testing.
 */
fun getCaCertSSLClientAcceptAll(): OkHttpClient {

    val trustManagers = arrayOf<TrustManager>(UnsafeTrustManager())

    // Install the all-trusting trust manager
    val sslContext: SSLContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustManagers, SecureRandom())
    // Create an ssl socket factory with our all-trusting manager
    val sslSocketFactory: SSLSocketFactory = sslContext.getSocketFactory()

    return OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustManagers[0] as X509TrustManager)
            .hostnameVerifier(HostnameVerifier { hostname, session -> true })
            .build()
}