package io.mainframe.hacs.PageFragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.EnumSet;
import java.util.Objects;

import io.mainframe.hacs.R;
import io.mainframe.hacs.components.DoorButtons;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;

public class StatusFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener, MqttStatusListener {

    private DoorButtons doorButtons;

    public StatusFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_status, container, false);

        doorButtons = (DoorButtons) view.findViewById(R.id.status_doorButtons);
        doorButtons.setOnButtonClickListener(new DoorButtons.OnButtonClickListener() {
            @Override
            public void onClick(DoorButtons.DoorButton doorButton, View view) {
                getInteraction().sendSshCommand(DoorCommand.getSwitchDoorStateCmd(doorButton.getStatus()));
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        PkCredentials credentials = new PkCredentials(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        // if the ssh key password is not set
        boolean readOnlyMode = !credentials.isPasswordSet();

        if (!readOnlyMode) {
            final NetworkStatus networkStatus = getInteraction().getNetworkStatus();
            networkStatus.addListener(this);
            doorButtons.setEnabled(networkStatus.isInMainframeWifi());
        } else {
            doorButtons.setEnabled(false);
        }

        final MqttConnector mqtt = getInteraction().getMqttConnector();
        mqtt.addListener(this, EnumSet.of(Topic.STATUS));

        setStatusText(mqtt.getLastStatus());
    }

    @Override
    public void onPause() {
        super.onPause();

        getInteraction().getMqttConnector().removeAllListener(this);
        getInteraction().getNetworkStatus().removeListener(this);
    }

    @Override
    public int getTitleRes() {
        return R.string.nav_status;
    }

    private void setStatusText(Status status) {
        TextView text = (TextView) getView().findViewById(R.id.status_status);
        text.setText(status == null ? getString(R.string.unknown) : status.getUiValue());
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi, boolean isInMainframeWifi) {
        doorButtons.setEnabled(isInMainframeWifi);
    }

    @Override
    public void onNewMsg(Topic topic, Object msg) {
        if (topic != Topic.STATUS) {
            return;
        }
        setStatusText((Status) msg);
    }

    @Override
    public void onMqttConnected() {
        // not needed
    }

    @Override
    public void onMqttConnectionLost() {
        setStatusText(null);
    }
}
