package io.mainframe.hacs.ssh;

import android.os.AsyncTask;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import org.pmw.tinylog.Logger;


/**
 * Checks the given private key
 */
public class CheckPrivateKeyAsync extends AsyncTask<String, Void, CheckPrivateKeyAsync.Result> {

    private final SshResponse<Result> responseHandler;

    public CheckPrivateKeyAsync(SshResponse<Result> responseHandler) {
        this.responseHandler = responseHandler;
    }

    @Override
    protected Result doInBackground(String... params) {
        final String privateKeyFile = params[0];
        if (privateKeyFile == null || privateKeyFile.isEmpty()) {
            return new Result(privateKeyFile, false, false);
        }

        JSch jsch = new JSch();
        try {
            final KeyPair keyPair = KeyPair.load(jsch, privateKeyFile);
            if (!keyPair.isEncrypted()) {
                return new Result(privateKeyFile, false, false);
            }

            if (params.length == 2) {
                // check also pw
                return new Result(privateKeyFile, true, keyPair.decrypt(params[1]));
            } else {
                return new Result(privateKeyFile, true, false);
            }

        } catch (JSchException e) {
            Logger.debug("KeyCheck excp: " + e.getMessage(), e);
            return new Result(null, false, false);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        super.onPostExecute(result);
        this.responseHandler.processFinish(result);
    }

    public static class Result {
        public final String privateKeyFile;
        public final boolean keyFileValid;
        public final boolean passwordMatch;

        public Result(String privateKeyFile, boolean keyFileValid, boolean passwordMatch) {
            this.privateKeyFile = privateKeyFile;
            this.keyFileValid = keyFileValid;
            this.passwordMatch = passwordMatch;
        }
    }
}
