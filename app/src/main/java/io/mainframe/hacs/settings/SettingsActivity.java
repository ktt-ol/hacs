package io.mainframe.hacs.settings;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.logging.LogConfig;
import io.mainframe.hacs.ssh.CheckPrivateKeyAsync;
import io.mainframe.hacs.ssh.KeyData;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.ssh.SshResponse;

public class SettingsActivity extends AppCompatPreferenceActivity implements EditTextWithScanPreference.ActivityRunner {

    public static final int RESULT_CHOOSE_PRIVATE_KEY = 2;

    private Map<Integer, EditTextWithScanPreference.ActivityResultCallback> callbacks = new ConcurrentHashMap<>();
    private int callbackIdCounter = 0;

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (!this.callbacks.containsKey(requestCode)) {
            return;
        }

        EditTextWithScanPreference.ActivityResultCallback callback = this.callbacks.remove(requestCode);
        if (resultCode == RESULT_OK) {
            String contents = data.getStringExtra("SCAN_RESULT");
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

            this.privateKeyFilename = findPreference(getString(R.string.PREFS_PRIVATE_KEY_FILENAME));
            this.privateKeyPassword = findPreference(getString(R.string.PREFS_PRIVATE_KEY_PASSWORD));
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            privateKeyFilename.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                startActivityForResult(intent, RESULT_CHOOSE_PRIVATE_KEY);
                return true;
            });

            this.privateKeyFilename.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String passwordValue = prefs.getString(
                            GeneralPreferenceFragment.this.privateKeyPassword.getKey(), null);
                    new CheckPrivateKeyAsync(GeneralPreferenceFragment.this)
                            .execute(new PkCredentials(KeyData.fromUri(Uri.parse((String) newValue), getActivity()), passwordValue));

                    return true;
                }
            });

            this.privateKeyPassword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String privateKeyFilenameValue = prefs.getString(
                            GeneralPreferenceFragment.this.privateKeyFilename.getKey(), null);

                    new CheckPrivateKeyAsync(GeneralPreferenceFragment.this)
                            .execute(new PkCredentials(KeyData.fromUri(Uri.parse(privateKeyFilenameValue), getActivity()), (String) newValue));

                    return true;
                }
            });

            final String debugLoggingKey = getString(R.string.PREFS_DEBUG_LOGGING);
            findPreference(debugLoggingKey).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    LogConfig.configureLogger((boolean) newValue);
                    return true;
                }
            });

            // run the validation on start
            new CheckPrivateKeyAsync(GeneralPreferenceFragment.this)
                    .execute(PkCredentials.fromSettings(getActivity()));
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == RESULT_CHOOSE_PRIVATE_KEY && resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        getActivity().getContentResolver().takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        prefs
                                .edit()
                                .putString(this.privateKeyFilename.getKey(), uri.toString())
                                .apply();

                        String passwordValue = prefs.getString(
                                GeneralPreferenceFragment.this.privateKeyPassword.getKey(), null);
                        new CheckPrivateKeyAsync(GeneralPreferenceFragment.this)
                                .execute(new PkCredentials(KeyData.fromUri(uri, getActivity()), passwordValue));

                    }
                }
            }
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
            if (!response.getKeyFileValid()) {
                this.privateKeyFilename.setSummary("Der private Schlüssel ist ungültig!");
                this.privateKeyPassword.setSummary("");
            } else {
                this.privateKeyFilename.setSummary(response.getPrivateKeyFile());
                if (!response.getPasswordMatch()) {
                    this.privateKeyPassword.setSummary("Das Passwort passt nicht für den gewählten privaten Schlüssel.");
                } else {
                    this.privateKeyPassword.setSummary("Das Passwort ist richtig.");
                }
            }
        }
    }
}
