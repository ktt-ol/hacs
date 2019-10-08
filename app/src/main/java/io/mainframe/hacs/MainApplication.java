package io.mainframe.hacs;

import android.app.Application;
import android.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import io.mainframe.hacs.common.logging.LogConfig;
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


        LogConfig.configureLogger(this);

        new TrashCalendar(this).setNextAlarm();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

    }
}
