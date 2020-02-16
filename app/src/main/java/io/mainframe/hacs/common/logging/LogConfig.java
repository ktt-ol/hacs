package io.mainframe.hacs.common.logging;

import android.content.Context;
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
        final boolean writeLogfile = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                context.getString(R.string.PREFS_WRITE_LOGFILE), false);

        configureLogger(writeLogfile);
    }

    public static void configureLogger(boolean writeLogfile) {
        final Configurator configurator = Configurator.defaultConfig()
                .formatPattern("{date} {level}: {class_name}.{method}()\t{message}");

        if (BuildConfig.DEBUG) {
            configurator
                    .writer(new LogcatWriter())
                    .level(Level.DEBUG);
        }

        if (writeLogfile) {
            final File folder = new File(Environment.getExternalStorageDirectory(), Constants.LOG_FILE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            final String logfile = new File(folder, "hacs.log").toString();
            // https://tinylog.org/configuration#LogcatWriter
            configurator
                    .writer(new RollingFileWriter(logfile, 10, new StartupPolicy(), new SizePolicy(1000 * 1000)))
                    .level(Level.DEBUG);
        }
        configurator.activate();

        Logger.info("Init logger, debug: {}, write log file: {}", BuildConfig.DEBUG, writeLogfile);
    }
}
