package io.mainframe.hacs

import android.app.Application
import io.mainframe.hacs.common.logging.LogConfig.configureLogger
import org.acra.ACRA
import org.acra.ReportingInteractionMode
import org.acra.annotation.ReportsCrashes

/**
 * Created by holger on 02.12.15.
 */
@ReportsCrashes(
    mailTo = "holgercremer@gmail.com",
    mode = ReportingInteractionMode.DIALOG,
    resDialogText = R.string.crash_dialog_text,
    resDialogIcon = android.R.drawable.ic_dialog_info,
    resDialogCommentPrompt = R.string.crash_dialog_comment_prompt,
    resDialogOkToast = R.string.crash_dialog_ok_toast
)
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // The following line triggers the initialization of ACRA
        ACRA.init(this)


        configureLogger(this)
    }
}
