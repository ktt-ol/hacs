package io.mainframe.hacs.ssh;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.mainframe.hacs.common.Constants.DoorServer;

/**
 * Created by holger on 09.11.15.
 */
public class RunSshAsync extends AsyncTask<Void, Void, RunSshAsync.Result> {
    private static final String TAG = "RunSSHCommand";
    private final SshResponse<Result> delegate;
    private final DoorServer server;
    private final PkCredentials credentials;
    private final DoorCommand command;
    private final boolean checkServerFingerprint;
    public RunSshAsync(SshResponse<Result> delegate, DoorServer server, PkCredentials credentials,
                       DoorCommand command, boolean checkServerFingerprint) {
        this.delegate = delegate;
        this.server = server;
        this.credentials = credentials;
        this.command = command;
        this.checkServerFingerprint = checkServerFingerprint;
    }

    @Override
    protected Result doInBackground(Void... voids) {
        JSch jsch = new JSch();
        try {
            jsch.addIdentity(this.credentials.privateKeyFile, this.credentials.password);
            Session session = jsch.getSession(this.server.user, this.server.host, this.server.port);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();
            final String hostKey = session.getHostKey().getFingerPrint(jsch);
            Log.d(TAG, "Server host key: " + hostKey);

            if (this.checkServerFingerprint && this.server.hostKey.compareToIgnoreCase(hostKey) != 0) {
                session.disconnect();
                String msg = String.format("Invalid host key. Expected '%s', but got '%s' instead.",
                        this.server.hostKey.toUpperCase(), hostKey.toUpperCase());
                Log.i(TAG, msg);

                return new Result(this.command.get(), Status.WRONG_HOST_KEY, msg);
            }

            // SSH Channel
            ChannelExec channelssh = (ChannelExec) session.openChannel("exec");

            ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
            channelssh.setErrStream(errorOut);

            // Execute command
            Log.d(TAG, "ssh exec: " + this.command.get());
            channelssh.setCommand(this.command.get());
            channelssh.connect();

            channelssh.start();

            String resultStr = readStream(channelssh.getInputStream());

            channelssh.disconnect();

            String errorStr = errorOut.toString("utf8");

            Log.d(TAG, "ssh output: " + resultStr);
            if (!errorStr.isEmpty()) {
                Log.w(TAG, "ssh error output: " + errorStr);
                return new Result(this.command.get(), Status.UNKNOWN_ERROR, errorStr);
            }
            return new Result(this.command.get(), Status.SUCCESS, resultStr);
        } catch (Exception e) {
            String msg = "Error running ssh: " + e.getMessage();
            Log.e(TAG, msg, e);
            return new Result(this.command.get(), Status.UNKNOWN_ERROR, msg);
        }
    }

    @Override
    protected void onPostExecute(Result result) {
        this.delegate.processFinish(result);
    }

    private String readStream(InputStream input) throws IOException {
        int data = input.read();
        StringBuilder outputBuffer = new StringBuilder();
        while (data != -1) {
            outputBuffer.append((char) data);
            data = input.read();
        }

        input.close();
        return outputBuffer.toString();
    }


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
}
