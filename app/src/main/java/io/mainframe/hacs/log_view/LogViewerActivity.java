package io.mainframe.hacs.log_view;

import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.pmw.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.Constants;

public class LogViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_log_viewer);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final boolean writeLogFiles = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                getString(R.string.PREFS_WRITE_LOGFILE), false);
        if (!writeLogFiles) {
            findViewById(R.id.logs_disabled_info).setVisibility(View.VISIBLE);
        }

        final File folder = new File(Environment.getExternalStorageDirectory(), Constants.LOG_FILE_FOLDER);
        if (folder.exists()) {
            final TextView contentTextView = findViewById(R.id.logs_content);
            final TabLayout tabView = findViewById(R.id.logs_tabview);

            tabView.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    StringBuilder content = new StringBuilder();
                    final File logFile = new File(folder, tab.getText().toString());
                    if (!logFile.exists()) {
                        contentTextView.setText("Log file not found?!");
                        return;
                    }

                    try {
                        BufferedReader br = new BufferedReader(new FileReader(logFile));
                        String line;

                        while ((line = br.readLine()) != null) {
                            content.append(line);
                            content.append('\n');
                        }
                        br.close();

                        contentTextView.setText(content.toString());
                    } catch (IOException e) {
                        Logger.error(e.getMessage(), e);
                        contentTextView.setText("Error: " + e.getMessage());
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // ignore
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    // ignore
                }
            });

            final String[] files = folder.list();
            if (files.length == 0) {
                contentTextView.setText("No log files found.");
                return;
            }
            for (String file : files) {
                final TabLayout.Tab tab = tabView.newTab();
                tab.setText(file);
                tabView.addTab(tab);
            }
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
