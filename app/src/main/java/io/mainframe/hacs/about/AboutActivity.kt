package io.mainframe.hacs.about

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.mainframe.hacs.BuildConfig
import io.mainframe.hacs.R

/**
 * Created by holger on 18.11.16.
 */
class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val textView = findViewById<View>(R.id.aboutVersion) as TextView
        textView.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        val libraries = arrayOf(
            arrayOf("EasyPermissions", "Apache License - Version 2.0"),
            arrayOf("ACRA", "Apache License - Version 2.0"),
            arrayOf("JSch - Java Secure Channel", "BSD-style license")
        )

        val layout = findViewById<View>(R.id.aboutLayout) as LinearLayout
        for (lib in libraries) {
            val name = TextView(this, null, R.style.LibName)
            name.text = lib[0]
            layout.addView(name)
            val license = TextView(this, null, R.style.LibLicense)
            license.text = lib[1]
            layout.addView(license)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        // go back?
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}