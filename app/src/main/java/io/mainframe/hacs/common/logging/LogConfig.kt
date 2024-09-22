package io.mainframe.hacs.common.logging

import android.content.Context
import android.preference.PreferenceManager
import io.mainframe.hacs.R
import org.pmw.tinylog.Configurator
import org.pmw.tinylog.Level
import org.pmw.tinylog.Logger
import org.pmw.tinylog.writers.LogcatWriter

object LogConfig {
    const val LOGGING_TAG: String = "Hacs"

    @JvmStatic
    fun configureLogger(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val debug = prefs.getBoolean(context.getString(R.string.PREFS_DEBUG_LOGGING), false)

        configureLogger(debug)
    }

    @JvmStatic
    fun configureLogger(debugLogging: Boolean) {
        Configurator.defaultConfig()
            .formatPattern("{date} {level}: {class_name}.{method}()\t{message}")
            .writer(LogcatWriter(LOGGING_TAG))
            .level(if (debugLogging) Level.DEBUG else Level.INFO)
            .activate()

        Logger.info("Init logger, debug enabled: {}.", debugLogging)
    }
}