package io.mainframe.hacs.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.DialogPreference;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import java.io.File;
import java.util.ArrayList;

import io.mainframe.hacs.R;

/**
 * Created by holger on 28.11.15.
 */
public class PrivateKeyChooser extends DialogPreference implements View.OnClickListener {

    private String privateKeyFilePath;
    private View contentView;

    public PrivateKeyChooser(Context context, AttributeSet attrs) {
        super(context, attrs);

        setDialogLayoutResource(R.layout.pref_private_key_chooser);
        setPositiveButtonText(android.R.string.ok);
        setNegativeButtonText(android.R.string.cancel);
        setDialogIcon(null);
    }

    @Override
    protected View onCreateDialogView() {
        this.contentView = super.onCreateDialogView();
        new FindPrivateKeysAsync().execute();
        return this.contentView;
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        if (restorePersistedValue) {
            // Restore existing state
            this.privateKeyFilePath = this.getPersistedString(null);
        }
    }


    @Override
    public void onClick(View v) {
        this.privateKeyFilePath = ((File) v.getTag()).getAbsolutePath();
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        // When the user selects "OK", persist the new value
        if (positiveResult) {
            if (callChangeListener(this.privateKeyFilePath)) {
                persistString(this.privateKeyFilePath);
            }
        }
    }


    private void dataRetrieved(ArrayList<File> result) {
        if (result.isEmpty()) {
            this.contentView.findViewById(R.id.listPrivateKeysProgress).setVisibility(View.GONE);
            this.contentView.findViewById(R.id.noPrivateKeysFound).setVisibility(View.VISIBLE);
            return;
        }

        RadioGroup privateKeyList = (RadioGroup) this.contentView.findViewById(R.id.privateKeyList);

        for (File keyFile : result) {
            final String absolutePath = keyFile.getAbsolutePath();
            RadioButton radioButton = new RadioButton(getContext());
            radioButton.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            radioButton.setPadding(0, 6, 0, 6);
            radioButton.setText(String.format("%s (%s)", keyFile.getName(), absolutePath));
            radioButton.setTag(keyFile);
            radioButton.setOnClickListener(this);
            privateKeyList.addView(radioButton);
            if (absolutePath.equals(this.privateKeyFilePath)) {
                privateKeyList.check(radioButton.getId());
            }
        }

        this.contentView.findViewById(R.id.listPrivateKeysProgress).setVisibility(View.GONE);
        this.contentView.findViewById(R.id.scrollView).setVisibility(View.VISIBLE);

    }


    public class FindPrivateKeysAsync extends AsyncTask<Void, Void, ArrayList<File>> {

        private final String[] IGNORE = new String[]{
                "sys", "proc", "etc", "system", "d", "acct", "vendor"
        };

        @Override
        protected ArrayList<File> doInBackground(Void... params) {
            String external_storage = Environment.getExternalStorageDirectory().getPath();
            File root = new File(external_storage);
            ArrayList<File> filesMatching = new ArrayList<File>();
            findFilesMatching(root, filesMatching);

            return filesMatching;
        }

        protected void onPostExecute(ArrayList<File> result) {
            PrivateKeyChooser.this.dataRetrieved(result);
        }

        private void findFilesMatching(File startDir, ArrayList<File> result) {
//            System.out.println("FOLDER: " + startDir.toString());
            for (String ignoreThis : IGNORE) {
                if (startDir.getName().equals(ignoreThis)) {
                    return;
                }
            }

            File[] fileList = startDir.listFiles();
            if (fileList == null) {
                return;
            }
            for (File file : fileList) {
                if (file.isDirectory()) {
                    findFilesMatching(file, result);
                    continue;
                }
//                System.out.println("FILE: " + file.toString());
                if (file.getName().startsWith("mf-door.") && file.getName().endsWith(".private.key")) {
                    result.add(file);
                }
            }
        }
    }
}
