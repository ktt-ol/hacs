package io.mainframe.hacs;

import android.app.Application;
import android.os.Environment;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.policies.SizePolicy;
import org.pmw.tinylog.policies.StartupPolicy;
import org.pmw.tinylog.writers.LogcatWriter;
import org.pmw.tinylog.writers.RollingFileWriter;

import java.io.File;

import io.mainframe.hacs.common.Constants;
import io.mainframe.hacs.trash_notifications.TrashCalendar;

/**
 * Created by holger on 02.12.15.
 */
@ReportsCrashes(
        mailTo = "holgercremer@gmail.com",
        mode = ReportingInteractionMode.DIALOG,

        resDialogText = R.string.crash_dialog_text,
        resDialogIcon = android.R.drawable.ic_dialog_info, //optional. default is a warning sign
        resDialogCommentPrompt = R.string.crash_dialog_comment_prompt, // optional. When defined, adds a user text field input with this text resource as a label
        resDialogOkToast = R.string.crash_dialog_ok_toast // optional. displays a Toast message when the user accepts to send a report.
)
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // The following line triggers the initialization of ACRA
        ACRA.init(this);

        final Configurator configurator = Configurator.defaultConfig()
                .formatPattern("{date} {level}: {class_name}.{method}()\t{message}");

        if (BuildConfig.DEBUG) {
            configurator
                    .writer(new LogcatWriter())
                    .level(Level.DEBUG);
        }

        final boolean writeLogfile = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                getString(R.string.PREFS_WRITE_LOGFILE), false);
        if (writeLogfile) {
            final File folder = new File(Environment.getExternalStorageDirectory(), Constants.LOG_FILE_FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            final String logfile = new File(folder, "hacs.log").toString();
            // https://tinylog.org/configuration#LogcatWriter
            configurator
                    .writer(new RollingFileWriter(logfile, 10, new StartupPolicy(), new SizePolicy(1000 * 1000)))
                    .level(Level.INFO);
        }
        configurator.activate();

        new TrashCalendar(this).setNextAlarm();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }
}
