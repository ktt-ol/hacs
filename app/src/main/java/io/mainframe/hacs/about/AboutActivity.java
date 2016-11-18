package io.mainframe.hacs.about;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.mainframe.hacs.BuildConfig;
import io.mainframe.hacs.R;

/**
 * Created by holger on 18.11.16.
 */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_about);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final TextView textView = (TextView) findViewById(R.id.aboutVersion);
        textView.setText(String.format("%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        String[][] libraries = new String[][]{
                new String[]{"Eclipse Paho Android Service", "Eclipse Public License - Version 1.0"},
                new String[]{"EasyPermissions", "Apache License - Version 2.0"},
                new String[]{"Simple Android File Chooser (by Roger Keays)", "Public Domain"},
                new String[]{"ACRA", "Apache License - Version 2.0"},
                new String[]{"JSch - Java Secure Channel", "BSD-style license"}
        };

        final LinearLayout layout = (LinearLayout) findViewById(R.id.aboutLayout);
        for (String[] lib : libraries) {
            TextView name = new TextView(this, null, R.attr.libNameStyle);
            name.setText(lib[0]);
            layout.addView(name);
            TextView license = new TextView(this, null, R.attr.libLicenseStyle);
            license.setText(lib[1]);
            layout.addView(license);
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        // go back?
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
