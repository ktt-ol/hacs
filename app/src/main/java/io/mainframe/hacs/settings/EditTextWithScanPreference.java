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

/**
 * Adds a "scan a qr code" button to the {@link EditTextPreference}. Not that the parent
 * {@link android.app.Activity} must implement the {@link ActivityRunner} interface.
 * Created by holger on 19.11.16.
 */

public class EditTextWithScanPreference extends EditTextPreference implements View.OnClickListener {

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
        scanButton.setText(getContext().getString(R.string.settings_scan_qrcode));
        scanButton.setOnClickListener(this);
        container.addView(scanButton);

    }

    @Override
    public void onClick(View v) {
        ActivityRunner activity = (ActivityRunner) getContext();
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            activity.startActivityWithResult(intent, new ActivityResultCallback() {
                @Override
                public void activityResultCallback(String result) {
                    getEditText().setText(result);
                }
            });
        } catch (ActivityNotFoundException anfe) {
            Log.i(TAG, "No QR code intent found.");
            // TODO: ask before open this intent
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            activity.startActivity(new Intent(Intent.ACTION_VIEW, marketUri));
        }
    }

    public interface ActivityRunner {

        void startActivityWithResult(Intent intent, ActivityResultCallback callback);

        void startActivity(Intent intent);

    }

    public interface ActivityResultCallback {
        void activityResultCallback(String result);
    }
}
