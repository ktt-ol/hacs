package io.mainframe.hacs.log_view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import io.mainframe.hacs.BuildConfig
import io.mainframe.hacs.R
import io.mainframe.hacs.common.logging.LogConfig
import org.pmw.tinylog.Logger
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class LogViewerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_log_viewer)
        setSupportActionBar(findViewById(R.id.logs_toolbar))

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val logsView = findViewById<TextView>(R.id.logs_content)
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v raw -s ${LogConfig.LOGGING_TAG}")
            BufferedReader(InputStreamReader(process.inputStream)).use { bufferedReader ->
                val log = StringBuilder()
                var line: String? = ""
                while (bufferedReader.readLine().also { line = it } != null) {
                    log.append(line).append('\n')
                }
                logsView.text = log.toString()
            }
        } catch (e: IOException) {
            val msg = "Can't get log from logcat: ${e.message}"
            Logger.error(msg, e)
            logsView.text = msg
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.log_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.log_copy_to_clipboard -> {
                copyToClipboard()
                true
            }
            R.id.log_export -> {
                sendLogPerMail()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyToClipboard() {
        // https://developer.android.com/guide/topics/text/copy-paste
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(
            "simple text",
            findViewById<TextView>(R.id.logs_content).text.toString()
        )
        clipboard.setPrimaryClip(clip)

        // https://developer.android.com/guide/topics/ui/notifiers/toasts
        Toast.makeText(applicationContext, "Log content copied.", Toast.LENGTH_SHORT)
            .show()

    }

    // https://developer.android.com/guide/components/intents-common#java
    private fun sendLogPerMail() {
        val logOutput = File(application.cacheDir, "log_export/${System.currentTimeMillis()}.log")
        logOutput.parentFile!!.mkdirs()
        val logsView = findViewById<TextView>(R.id.logs_content)
        logOutput.writeText(logsView.text.toString())
        val logUri = FileProvider.getUriForFile(applicationContext,
            "${BuildConfig.APPLICATION_ID}.fileprovider", logOutput)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "plain/text"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("holgercremer@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Hacs Log")
            putExtra(Intent.EXTRA_TEXT, "Anbei das Log file...")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, logUri)
        }
        startActivity(intent)
    }
}