package io.mainframe.hacs.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.YesNoDialog;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.ssh.RunSshAsync;
import io.mainframe.hacs.ssh.SshResponse;

/**
 * Submits ssh commands and shows a progress bar meanwhile.
 */
public class SshUiHandler extends Fragment implements SshResponse<RunSshAsync.Result>, YesNoDialog.ResultListener {

    private OnShhCommandHandler mListener;

    private String tryCommand;

    public SshUiHandler() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ssh_ui_handler, container, false);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        System.out.println("SSH UI ON RESUME");
    }


    public void runSshCommand(String command) {
        this.tryCommand = command;

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        PkCredentials credentials = new PkCredentials(preferences);
        getView().findViewById(R.id.sshUiProgress).setVisibility(View.VISIBLE);
        new RunSshAsync(this, this.tryCommand, true).execute(credentials);
    }

    /**
     * When a 'RunSshAsync' task is completed
     */
    @Override
    public void processFinish(RunSshAsync.Result response) {
        switch (response.status) {
            case SUCCESS:
                Toast.makeText(getContext(), response.msg, Toast.LENGTH_LONG).show();
                actionDone(true);
                break;
            case WRONG_HOST_KEY:
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                boolean checkServerFingerprint = preferences.getBoolean("checkServerFingerprint", true);
                if (checkServerFingerprint) {
                    Toast.makeText(getContext(), response.msg, Toast.LENGTH_LONG).show();
                    break;
                }
                String dialogMsg = response.msg + "\nContinue?";
                YesNoDialog.show(getContext(), "Wrong Hostkey", dialogMsg, "hostkey", this);
                break;
            case UNKNOWN_ERROR:
                Toast.makeText(getContext(), response.msg, Toast.LENGTH_LONG).show();
                actionDone(false);
                break;
        }
    }

    @Override
    public void dialogClosed(String tag, boolean resultOk) {
        if (resultOk && tag.equals("hostkey")) {
            // try the last command again
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            PkCredentials credentials = new PkCredentials(preferences);
            new RunSshAsync(this, this.tryCommand, false).execute(credentials);
        } else {
            actionDone(false);
        }
    }

    private void actionDone(boolean result) {
        getView().findViewById(R.id.sshUiProgress).setVisibility(View.INVISIBLE);
        this.mListener.onSshCommandComplete(this.tryCommand, result);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnShhCommandHandler) {
            this.mListener = (OnShhCommandHandler) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnShhCommandHandler");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnShhCommandHandler {
        void onSshCommandComplete(String command, boolean success);
    }

}
