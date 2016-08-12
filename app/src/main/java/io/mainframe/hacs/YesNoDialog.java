package io.mainframe.hacs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by holger on 14.11.15.
 */
public class YesNoDialog extends DialogFragment {

    interface ResultListener {
        void dialogClosed(String tag, boolean resultOk);
    }


    public static YesNoDialog create(String title, String message) {
        YesNoDialog dialog = new YesNoDialog();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        dialog.setArguments(args);

        return dialog;
    }


    private ResultListener listener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        String title = args.getString("title", "");
        String message = args.getString("message", "");

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        YesNoDialog.this.listener.dialogClosed(getTag(), true);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        YesNoDialog.this.listener.dialogClosed(getTag(), false);
                    }
                })
                .create();
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            this.listener = (ResultListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString() + " must implement " + ResultListener.class.getSimpleName());
        }
    }
}