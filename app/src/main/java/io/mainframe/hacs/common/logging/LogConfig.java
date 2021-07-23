package io.mainframe.hacs.common.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.LogcatWriter;

import io.mainframe.hacs.R;

public class LogConfig {

    public static final String LOGGING_TAG = "Hacs";

    public static void configureLogger(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean debug = prefs.getBoolean(context.getString(R.string.PREFS_DEBUG_LOGGING), false);

        configureLogger(debug);
    }

    public static void configureLogger(boolean debugLogging) {
        Configurator.defaultConfig()
                .formatPattern("{date} {level}: {class_name}.{method}()\t{message}")
                .writer(new LogcatWriter(LOGGING_TAG))
                .level(debugLogging ? Level.DEBUG : Level.INFO)
                .activate();

        Logger.info("Init logger, debug enabled: {}.", debugLogging);
    }
}
