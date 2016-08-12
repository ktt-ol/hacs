package io.mainframe.hacs.ssh;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Created by holger on 29.11.15.
 */
public class PkCredentials {

    public final String privateKeyFile;
    public final String password;

    public PkCredentials(SharedPreferences sharedPreferences) {
        this.privateKeyFile = sharedPreferences.getString("privateKeyFilename", null);
        this.password = sharedPreferences.getString("privateKeyPassword", null);
    }

    /**
     * Gets the "lalafoo" from "mf-door.lalafoo.private.key"
     *
     * @return the user, based on the name of the key file
     */
    public String getUser() {
        File pkFile = new File(this.privateKeyFile);
        String[] splits = pkFile.getName().split(Pattern.quote("."));
        if (splits.length != 4) {
            throw new IllegalArgumentException("Invalid private key filename: " + pkFile.getName());
        }
        return splits[1];
    }
}
