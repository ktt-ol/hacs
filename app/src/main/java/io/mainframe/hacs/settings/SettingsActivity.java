package io.mainframe.hacs.settings;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.util.Pair;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.mainframe.hacs.R;
import io.mainframe.hacs.ssh.CheckPrivateKeyAsync;
import io.mainframe.hacs.ssh.SshResponse;

public class SettingsActivity extends AppCompatPreferenceActivity implements EditTextWithScanPreference.ActivityRunner {

    private Map<Integer, EditTextWithScanPreference.ActivityResultCallback> callbacks = new ConcurrentHashMap<>();
    private int callbackIdCounter = 0;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!this.callbacks.containsKey(requestCode)) {
            return;
        }

        EditTextWithScanPreference.ActivityResultCallback callback = this.callbacks.remove(requestCode);
        if (resultCode == RESULT_OK) {
            String contents = data.getStringExtra("SCAN_RESULT");
            System.out.println("contens: " + contents);
            callback.activityResultCallback(contents);
        }
//        if (resultCode == RESULT_CANCELED) {
//            handle cancel
//        }
    }

    @Override
    public void startActivityWithResult(Intent intent, EditTextWithScanPreference.ActivityResultCallback callback) {
        int id = this.callbackIdCounter;
        this.callbackIdCounter++;

        this.callbacks.put(id, callback);
        startActivityForResult(intent, id);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        getFragmentManager().beginTransaction().replace(android.R.id.content, new GeneralPreferenceFragment()).commit();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }


    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment implements SshResponse<CheckPrivateKeyAsync.Result> {

        private Preference privateKeyFilename;
        private Preference privateKeyPassword;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            this.privateKeyFilename = findPreference("privateKeyFilename");
            this.privateKeyPassword = findPreference("privateKeyPassword");
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            this.privateKeyFilename.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String passwordValue = prefs
                            .getString(GeneralPreferenceFragment.this.privateKeyPassword.getKey(), null);
                    new CheckPrivateKeyAsync(GeneralPreferenceFragment.this).execute((String) newValue, passwordValue);

                    return true;
                }
            });

            this.privateKeyPassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String privateKeyFilenameValue = prefs
                            .getString(GeneralPreferenceFragment.this.privateKeyFilename.getKey(), null);
                    new CheckPrivateKeyAsync(GeneralPreferenceFragment.this).execute(privateKeyFilenameValue, (String) newValue);

                    return true;
                }
            });

            // run the validation on start
            new CheckPrivateKeyAsync(GeneralPreferenceFragment.this).execute(
                    prefs.getString(this.privateKeyFilename.getKey(), null),
                    prefs.getString(this.privateKeyPassword.getKey(), null)
            );
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                getActivity().finish();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void processFinish(CheckPrivateKeyAsync.Result response) {
            boolean credentialsOk = false;
            if (!response.keyFileValid) {
                this.privateKeyFilename.setSummary("KeyFile is invalid!");
                this.privateKeyPassword.setSummary("");
            } else {
                this.privateKeyFilename.setSummary(response.privateKeyFile);
                if (!response.passwordMatch) {
                    this.privateKeyPassword.setSummary("Password is not correct for the selected private key.");
                } else {
                    credentialsOk = true;
                    this.privateKeyPassword.setSummary("Password is correct.");
                }
            }
            final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
            editor.putBoolean("credentialsOk", credentialsOk);
            editor.commit();

        }
    }
}
