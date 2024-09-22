package io.mainframe.hacs.settings

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.preference.Preference
import android.preference.Preference.OnPreferenceChangeListener
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.MenuItem
import io.mainframe.hacs.R
import io.mainframe.hacs.common.logging.LogConfig
import io.mainframe.hacs.settings.EditTextWithScanPreference.ActivityResultCallback
import io.mainframe.hacs.ssh.CheckPrivateKeyAsync
import io.mainframe.hacs.ssh.KeyData
import io.mainframe.hacs.ssh.PkCredentials
import org.pmw.tinylog.Logger
import java.util.concurrent.ConcurrentHashMap

class SettingsActivity : AppCompatPreferenceActivity(), EditTextWithScanPreference.ActivityRunner {
    private val callbacks: MutableMap<Int, ActivityResultCallback> = ConcurrentHashMap()
    private var callbackIdCounter = 0

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (!callbacks.containsKey(requestCode)) {
            return
        }

        val callback = callbacks.remove(requestCode)
        if (resultCode == RESULT_OK) {
            val contents = data?.getStringExtra("SCAN_RESULT")
            callback?.activityResultCallback(contents)
        }
    }

    override fun startActivityWithResult(intent: Intent, callback: ActivityResultCallback) {
        val id = this.callbackIdCounter
        callbackIdCounter++

        callbacks[id] = callback
        startActivityForResult(intent, id)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()

        fragmentManager.beginTransaction()
            .replace(android.R.id.content, GeneralPreferenceFragment()).commit()
    }

    /**
     * Set up the [android.app.ActionBar], if the API is available.
     */
    private fun setupActionBar() {
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onIsMultiPane(): Boolean = isXLargeTablet(this)

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName || GeneralPreferenceFragment::class.java.name == fragmentName
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    class GeneralPreferenceFragment : PreferenceFragment() {
        private var privateKeyFilename: Preference? = null
        private var privateKeyPassword: Preference? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)

            val privateKeyFilename = findPreference(getString(R.string.PREFS_PRIVATE_KEY_FILENAME))
            this.privateKeyFilename = privateKeyFilename
            val privateKeyPassword = findPreference(getString(R.string.PREFS_PRIVATE_KEY_PASSWORD))
            this.privateKeyPassword = privateKeyPassword
            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

            privateKeyFilename.onPreferenceClickListener = OnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("*/*")
                startActivityForResult(intent, RESULT_CHOOSE_PRIVATE_KEY)
                true
            }

            privateKeyFilename.onPreferenceChangeListener =
                OnPreferenceChangeListener { _, newValue ->
                    val passwordValue = prefs.getString(
                        privateKeyPassword.key, null
                    )
                    if (passwordValue == null) {
                        Logger.warn("passwordValue is 'null'!")
                        return@OnPreferenceChangeListener false
                    }

                    CheckPrivateKeyAsync(this::onSshFinish)
                        .execute(
                            PkCredentials(
                                KeyData.fromUri(Uri.parse(newValue as String), activity),
                                passwordValue
                            )
                        )
                    true
                }

            privateKeyPassword.onPreferenceChangeListener =
                OnPreferenceChangeListener { _, newValue ->
                    val privateKeyFilenameValue = prefs.getString(
                        privateKeyFilename.key, null
                    )
                    if (privateKeyFilenameValue == null) {
                        Logger.warn("privateKeyFilenameValue is 'null'!")
                        return@OnPreferenceChangeListener false
                    }

                    CheckPrivateKeyAsync(this::onSshFinish)
                        .execute(
                            PkCredentials(
                                KeyData.fromUri(
                                    Uri.parse(privateKeyFilenameValue),
                                    activity
                                ), newValue as String
                            )
                        )
                    true
                }

            val debugLoggingKey = getString(R.string.PREFS_DEBUG_LOGGING)
            findPreference(debugLoggingKey).onPreferenceChangeListener =
                OnPreferenceChangeListener { _, newValue ->
                    LogConfig.configureLogger(newValue as Boolean)
                    true
                }

            // run the validation on start
            CheckPrivateKeyAsync(this::onSshFinish)
                .execute(PkCredentials.fromSettings(activity))
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == RESULT_CHOOSE_PRIVATE_KEY && resultCode == RESULT_OK) {
                val uri = data?.data
                if (uri != null) {
                    activity.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
                    prefs
                        .edit()
                        .putString(privateKeyFilename!!.key, uri.toString())
                        .apply()

                    val passwordValue = prefs.getString(
                        privateKeyPassword!!.key, null
                    )
                    CheckPrivateKeyAsync(this::onSshFinish)
                        .execute(PkCredentials(KeyData.fromUri(uri, activity), passwordValue))
                }
            }
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                activity.finish()
                return true
            }
            return super.onOptionsItemSelected(item)
        }

        private fun onSshFinish(response: CheckPrivateKeyAsync.Result) {
            if (!response.keyFileValid) {
                privateKeyFilename!!.summary = "Der private Schlüssel ist ungültig!"
                privateKeyPassword!!.summary = ""
            } else {
                privateKeyFilename!!.summary = response.privateKeyFile
                if (!response.passwordMatch) {
                    privateKeyPassword!!.summary =
                        "Das Passwort passt nicht für den gewählten privaten Schlüssel."
                } else {
                    privateKeyPassword!!.summary = "Das Passwort ist richtig."
                }
            }
        }
    }

    companion object {
        const val RESULT_CHOOSE_PRIVATE_KEY: Int = 2

        /**
         * Helper method to determine if the device has an extra-large screen. For
         * example, 10" tablets are extra-large.
         */
        private fun isXLargeTablet(context: Context): Boolean {
            return (context.resources.configuration.screenLayout
                    and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }
    }
}