package io.mainframe.hacs.ssh

import com.jcraft.jsch.JSch
import io.mainframe.hacs.common.Constants
import io.mainframe.hacs.common.Constants.MACHINING_DOOR
import io.mainframe.hacs.common.Constants.SPACE_DOOR_BACK
import io.mainframe.hacs.common.Constants.SPACE_DOOR_FRONT
import io.mainframe.hacs.common.Constants.WOODWORKING_DOOR_BACK
import io.mainframe.hacs.common.Constants.WOODWORKING_DOOR_FRONT
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.readBytes

class KeyCheckTests {

    @Test
    @Ignore("You need a local key file and access to the server...")
    fun testAllKeys() {
        val keyFile = Path("your key file...")
        val password = ""

        val allServer = listOf(
            SPACE_DOOR_FRONT,
            SPACE_DOOR_BACK,
            MACHINING_DOOR,
            WOODWORKING_DOOR_FRONT,
            WOODWORKING_DOOR_BACK,
        )

        allServer.forEach {
            testServer(it, keyFile, password)
        }
    }

    private fun testServer(
        server: Constants.DoorServer,
        privateKeyFile: Path,
        privateKeyPassword: String
    ) {
        val jsch = JSch()
        jsch.addIdentity(
            privateKeyFile.name,
            privateKeyFile.readBytes(),
            null,
            privateKeyPassword.toByteArray()
        )

        val session = jsch.getSession(server.user, server.host, server.port)
        val prop = Properties().apply {
            this["StrictHostKeyChecking"] = "no"
        }
        session.setConfig(prop)
        session.connect()

        val hostKey = session.hostKey.getFingerPrint(jsch)
        assertThat(
            "Server: $server",
            hostKey.lowercase(),
            CoreMatchers.equalTo(server.hostKey.lowercase())
        )

        session.disconnect()
    }
}