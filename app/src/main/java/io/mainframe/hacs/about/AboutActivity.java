package io.mainframe.hacs.about;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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
        textView.setText(String.format("Version: %s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
    }
}
