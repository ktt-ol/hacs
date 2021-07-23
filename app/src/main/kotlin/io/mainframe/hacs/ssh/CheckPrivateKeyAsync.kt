package io.mainframe.hacs.ssh

import io.mainframe.hacs.ssh.SshResponse
import android.os.AsyncTask
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.KeyPair
import org.pmw.tinylog.Logger

/**
 * Checks the given private key
 */
class CheckPrivateKeyAsync(private val responseHandler: SshResponse<Result>) :
    AsyncTask<PkCredentials, Void, CheckPrivateKeyAsync.Result>() {
    override fun doInBackground(vararg params: PkCredentials): Result {
        val credentials = params[0]
        if (credentials.privateKey == null) {
            Logger.debug("No or empty private key file.")
            return Result(null, false, false)
        }
        val jsch = JSch()
        return try {
            val keyPair = KeyPair.load(jsch, credentials.privateKey.data, null)
            if (!keyPair.isEncrypted) {
                Logger.debug("!keyPair.isEncrypted()")
                return Result(credentials.privateKey.name, false, false)
            }
            if (credentials.password != null) {
                // check also pw
                Result(credentials.privateKey.name, true, keyPair.decrypt(credentials.password))
            } else {
                Result(credentials.privateKey.name, true, false)
            }
        } catch (e: JSchException) {
            Logger.debug(e, "KeyCheck excp: " + e.message)
            Result(null, false, false)
        }
    }

    override fun onPostExecute(result: Result) {
        super.onPostExecute(result)
        responseHandler.processFinish(result)
    }

    data class Result(val privateKeyFile: String?, val keyFileValid: Boolean, val passwordMatch: Boolean)
}
