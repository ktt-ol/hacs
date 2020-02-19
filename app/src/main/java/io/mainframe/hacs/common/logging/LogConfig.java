package io.mainframe.hacs.common.logging;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.policies.SizePolicy;
import org.pmw.tinylog.policies.StartupPolicy;
import org.pmw.tinylog.writers.LogcatWriter;
import org.pmw.tinylog.writers.RollingFileWriter;

import java.io.File;

import io.mainframe.hacs.BuildConfig;
import io.mainframe.hacs.R;
import io.mainframe.hacs.common.Constants;

public class LogConfig {

    public static void configureLogger(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean writeLogfile = prefs.getBoolean(context.getString(R.string.PREFS_ENABLE_LOGGING), false);
        final boolean debug = prefs.getBoolean(context.getString(R.string.PREFS_DEBUG_LOGGING), false);

        configureLogger(writeLogfile, debug);
    }

    public static void configureLogger(boolean enableLogging, boolean debugLogging) {
        if (!enableLogging) {
            Configurator.defaultConfig()
                    .removeAllWriters()
                    .activate();
            return;
        }

        final File folder = new File(Environment.getExternalStorageDirectory(), Constants.LOG_FILE_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        final String logfile = new File(folder, "hacs.log").toString();

        Configurator.defaultConfig()
                .formatPattern("{date} {level}: {class_name}.{method}()\t{message}")
                .writer(new LogcatWriter("FOO"))
                .level(debugLogging ? Level.DEBUG : Level.INFO)
                .addWriter(new RollingFileWriter(logfile, 10, new StartupPolicy(), new SizePolicy(1000 * 1000)))
                .activate();

        Logger.info("Init logger, debug enabled: {}.", debugLogging);
    }
}
