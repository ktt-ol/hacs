package io.mainframe.hacs.common

import android.content.Context
import android.support.v7.app.AlertDialog
import io.mainframe.hacs.R

/**
 * Created by holger on 14.11.15.
 */
object YesNoDialog {
    @JvmStatic
    fun show(
        context: Context,
        title: String,
        message: String,
        tag: String,
        callbackListener: (tag: String, resultOk: Boolean) -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.yes) { _, _ -> callbackListener(tag, true) }
            .setNegativeButton(R.string.no) { _, _ -> callbackListener(tag, false) }
            .show()
    }
}
