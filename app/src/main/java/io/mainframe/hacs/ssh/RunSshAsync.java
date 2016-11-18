package io.mainframe.hacs.ssh;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.InputStream;
import java.util.Properties;

import io.mainframe.hacs.common.Constants;

/**
 * Created by holger on 09.11.15.
 */
public class RunSshAsync extends AsyncTask<PkCredentials, Void, RunSshAsync.Result> {
    private static final String TAG = "RunSSHCommand";

    public enum Status {
        SUCCESS, UNKNOWN_ERROR, WRONG_HOST_KEY
    }

    public static final class Result {
        public final String command;
        public final Status status;
        public final String msg;

        public Result(String command, Status status, String msg) {
            this.command = command;
            this.status = status;
            this.msg = msg;
        }
    }

    private final SshResponse<Result> delegate;
    private final String command;
    private final boolean checkServerFingerprint;

    public RunSshAsync(SshResponse<Result> delegate, String command, boolean checkServerFingerprint) {
        this.delegate = delegate;
        this.command = command;
        this.checkServerFingerprint = checkServerFingerprint;
    }

    @Override
    protected Result doInBackground(PkCredentials... params) {
        PkCredentials credentials = params[0];

        JSch jsch = new JSch();
        try {
            jsch.addIdentity(credentials.privateKeyFile, credentials.password);
            Session session = jsch.getSession(Constants.DOOR_USER, Constants.DOOR_SERVER_HOST, Constants.DOOR_SERVER_PORT);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();
            final String hostKey = session.getHostKey().getFingerPrint(jsch);
            Log.d(TAG, "Server host key: " + hostKey);

            if (this.checkServerFingerprint &&
                    Constants.DOOR_SERVER_HOST_KEY.compareToIgnoreCase(hostKey) != 0) {
                session.disconnect();
                String msg = String.format("Invalid host key. Expected '%s', but got '%s' instead.",
                        Constants.DOOR_SERVER_HOST_KEY.toUpperCase(), hostKey.toUpperCase());
                Log.i(TAG, msg);

                return new Result(this.command, Status.WRONG_HOST_KEY, msg);
            }

            // SSH Channel
            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");

            // Execute command
            channelssh.setCommand(this.command);
            channelssh.connect();

            InputStream input = channelssh.getInputStream();
            int data = input.read();
            StringBuilder outputBuffer = new StringBuilder();
            while (data != -1) {
                outputBuffer.append((char) data);
                data = input.read();
            }

            channelssh.disconnect();

            return new Result(this.command, Status.SUCCESS, outputBuffer.toString());
        } catch (Exception e) {
            String msg = "Error running ssh: " + e.getMessage();
            Log.e(TAG, msg, e);
            return new Result(this.command, Status.UNKNOWN_ERROR, msg);
        }
    }


    @Override
    protected void onPostExecute(Result result) {
        this.delegate.processFinish(result);
    }

}
