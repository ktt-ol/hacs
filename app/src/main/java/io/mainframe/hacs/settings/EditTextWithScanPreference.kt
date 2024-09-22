package io.mainframe.hacs.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.preference.EditTextPreference
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import io.mainframe.hacs.R
import io.mainframe.hacs.common.YesNoDialog
import org.pmw.tinylog.Logger

/**
 * Adds a "scan a qr code" button to the [EditTextPreference]. Not that the parent
 * [android.app.Activity] must implement the [ActivityRunner] interface.
 * Created by holger on 19.11.16.
 */
class EditTextWithScanPreference : EditTextPreference, View.OnClickListener {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onAddEditTextToDialogView(dialogView: View, editText: EditText) {
        super.onAddEditTextToDialogView(dialogView, editText)
        val container = editText.parent as ViewGroup

        val scanButton = Button(context)
        scanButton.text = context.getString(R.string.settings_qrcode_scan)
        scanButton.setOnClickListener(this)
        container.addView(scanButton)
    }

    override fun onClick(v: View) {
        val activity = context as ActivityRunner
        try {
            val intent = Intent(INTENT_QR_CODE_SCAN)
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
            activity.startActivityWithResult(intent, object : ActivityResultCallback {
                override fun activityResultCallback(result: String?) {
                    editText.setText(result)
                }
            })
        } catch (anfe: ActivityNotFoundException) {
            Logger.info("No QR code intent found.")
            YesNoDialog.show(
                context,
                context.getString(R.string.settings_qrcode_install_app_title),
                context.getString(R.string.settings_qrcode_install_app),
                ""
            ) { _: String, resultOk: Boolean ->
                if (resultOk) {
                    val marketUri = Uri.parse(MARKET_LINK_QR_CODE_SCAN)
                    activity.startActivity(Intent(Intent.ACTION_VIEW, marketUri))
                }
            }.show()
        }
    }

    interface ActivityRunner {
        fun startActivityWithResult(intent: Intent, callback: ActivityResultCallback)
        fun startActivity(intent: Intent)
    }

    interface ActivityResultCallback {
        fun activityResultCallback(result: String?)
    }

    companion object {
        const val INTENT_QR_CODE_SCAN: String = "com.google.zxing.client.android.SCAN"
        const val MARKET_LINK_QR_CODE_SCAN: String =
            "market://details?id=com.google.zxing.client.android"
    }
}