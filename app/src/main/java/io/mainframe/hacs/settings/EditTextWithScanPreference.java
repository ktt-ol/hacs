package io.mainframe.hacs.settings;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.EditTextPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.YesNoDialog;

/**
 * Adds a "scan a qr code" button to the {@link EditTextPreference}. Not that the parent
 * {@link android.app.Activity} must implement the {@link ActivityRunner} interface.
 * Created by holger on 19.11.16.
 */

public class EditTextWithScanPreference extends EditTextPreference implements View.OnClickListener, YesNoDialog.ResultListener {

    public static final String INTENT_QR_CODE_SCAN = "com.google.zxing.client.android.SCAN";
    public static final String MARKET_LINK_QR_CODE_SCAN = "market://details?id=com.google.zxing.client.android";

    private static final String TAG = EditTextWithScanPreference.class.getName();

    public EditTextWithScanPreference(Context context) {
        super(context);
    }

    public EditTextWithScanPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextWithScanPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public EditTextWithScanPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        ViewGroup container = (ViewGroup) editText.getParent();

        Button scanButton = new Button(getContext());
        scanButton.setText(getContext().getString(R.string.settings_qrcode_scan));
        scanButton.setOnClickListener(this);
        container.addView(scanButton);

    }

    @Override
    public void onClick(View v) {
        ActivityRunner activity = (ActivityRunner) getContext();
        try {
            Intent intent = new Intent(INTENT_QR_CODE_SCAN);
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            activity.startActivityWithResult(intent, new ActivityResultCallback() {
                @Override
                public void activityResultCallback(String result) {
                    getEditText().setText(result);
                }
            });
        } catch (ActivityNotFoundException anfe) {
            Log.i(TAG, "No QR code intent found.");
            YesNoDialog.show(getContext(),
                    getContext().getString(R.string.settings_qrcode_install_app_title),
                    getContext().getString(R.string.settings_qrcode_install_app),
                    null, this)
                    .show();
        }
    }

    @Override
    public void dialogClosed(String tag, boolean resultOk) {
        if (!resultOk) {
            return;
        }
        Uri marketUri = Uri.parse(MARKET_LINK_QR_CODE_SCAN);
        ActivityRunner activity = (ActivityRunner) getContext();
        activity.startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
    }


    public interface ActivityRunner {

        void startActivityWithResult(Intent intent, ActivityResultCallback callback);

        void startActivity(Intent intent);

    }

    public interface ActivityResultCallback {
        void activityResultCallback(String result);
    }
}
