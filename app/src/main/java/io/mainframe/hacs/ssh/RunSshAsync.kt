package io.mainframe.hacs.ssh

import android.os.AsyncTask
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import io.mainframe.hacs.common.Constants.DoorServer
import org.pmw.tinylog.Logger
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.Properties

/**
 * Created by holger on 09.11.15.
 */
class RunSshAsync(
    private val onSshFinish: (Result) -> Unit,
    private val server: DoorServer,
    private val credentials: PkCredentials,
    private val command: DoorCommand,
    private val checkServerFingerprint: Boolean
) : AsyncTask<Void, Void, RunSshAsync.Result>() {

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg voids: Void): Result {
        val jsch = JSch()
        try {
            val privateKey =
                checkNotNull(credentials.privateKey) { "Expected a private key to be set here." }
            val password =
                checkNotNull(credentials.password) { "Expected a password to be set here." }
            jsch.addIdentity(
                privateKey.name,
                privateKey.data,
                null,
                password.toByteArray()
            )
            val session = jsch.getSession(server.user, server.host, server.port)

            // Avoid asking for key confirmation
            val prop = Properties().apply {
                this["StrictHostKeyChecking"] = "no"
            }
            session.setConfig(prop)
            session.connect()

            val hostKey = session.hostKey.getFingerPrint(jsch)
            Logger.debug("Server host key: {}", hostKey)

            if (this.checkServerFingerprint && !server.hostKey.equals(hostKey, ignoreCase = true)) {
                session.disconnect()
                val msg =
                    "Invalid host key. Expected '${server.hostKey.uppercase(Locale.getDefault())}', " +
                            "but got '${hostKey.uppercase(Locale.getDefault())}' instead."
                Logger.info(msg)
                return Result(command.get(), Status.WRONG_HOST_KEY, msg)
            }

            // SSH Channel
            val channelssh = session.openChannel("exec") as ChannelExec

            val errorOut = ByteArrayOutputStream()
            channelssh.setErrStream(errorOut)

            // Execute command
            Logger.debug("ssh exec: {}", command.get())
            channelssh.setCommand(command.get())
            channelssh.connect()
            channelssh.start()
            val resultStr = channelssh.inputStream.bufferedReader().use { it.readText() }
            channelssh.disconnect()

            val errorStr = errorOut.toString("utf8")
            Logger.debug("ssh output: {}", resultStr)
            if (errorStr.isNotEmpty()) {
                Logger.warn("ssh error output: {}, ", errorStr)
                return Result(command.get(), Status.UNKNOWN_ERROR, errorStr)
            }
            return Result(command.get(), Status.SUCCESS, resultStr)
        } catch (e: Exception) {
            val msg = "Error running ssh: ${e.message}"
            Logger.error(e, msg)
            return Result(command.get(), Status.UNKNOWN_ERROR, msg)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: Result) {
        onSshFinish(result)
    }

    enum class Status {
        SUCCESS, UNKNOWN_ERROR, WRONG_HOST_KEY
    }

    data class Result(val command: String, val status: Status, val msg: String)
}
