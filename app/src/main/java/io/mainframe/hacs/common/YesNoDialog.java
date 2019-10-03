package io.mainframe.hacs.common;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import io.mainframe.hacs.R;

/**
 * Created by holger on 14.11.15.
 */
public class YesNoDialog {

    public static AlertDialog show(Context context, String title, String message, final String tag, final ResultListener callbackListener) {

        return new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callbackListener.dialogClosed(tag, true);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callbackListener.dialogClosed(tag, false);
                    }
                })
                .show();
    }

    public interface ResultListener {
        void dialogClosed(String tag, boolean resultOk);
    }
}