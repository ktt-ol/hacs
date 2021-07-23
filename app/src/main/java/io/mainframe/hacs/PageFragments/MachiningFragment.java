package io.mainframe.hacs.PageFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.EnumSet;

import io.mainframe.hacs.R;
import io.mainframe.hacs.components.DoorButtons;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;

import static io.mainframe.hacs.common.Constants.MACHINING_DOOR;

/**
 * Created by holger on 21.05.18.
 */

public class MachiningFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener, MqttStatusListener {

    private DoorButtons doorButtons;
    // if the mqtt password is not set
    private boolean readOnlyMode = false;

    public MachiningFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_machining, container, false);
        doorButtons = view.findViewById(R.id.machining_doorButtons);
        doorButtons.setOnButtonClickListener(new DoorButtons.OnButtonClickListener() {
            @Override
            public void onClick(DoorButtons.DoorButton doorButton, View view) {
                getInteraction().sendSshCommand(MACHINING_DOOR, DoorCommand.getSwitchDoorStateCmd(doorButton.getStatus()));
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // if the ssh key password is not set
        boolean readOnlyMode = !PkCredentials.isPasswordSet(getContext());

        if (!readOnlyMode) {
            final NetworkStatus networkStatus = getInteraction().getNetworkStatus();
            networkStatus.addListener(this);

            if (networkStatus.isRequireMainframeWifi()) {
                doorButtons.setEnabled(networkStatus.isInMainframeWifi() && networkStatus.hasMachiningBssid());
            } else {
                doorButtons.setEnabled(true);
            }
        } else {
            doorButtons.setEnabled(false);
        }

        final MqttConnector mqtt = getInteraction().getMqttConnector();
        mqtt.addListener(this, EnumSet.of(Topic.STATUS_MACHINING, Topic.KEYHOLDER_MACHINING));

        setStatusText(mqtt.getLastValue(Topic.STATUS_MACHINING, Status.class));
        setKeyholderText(mqtt.getLastValue(Topic.KEYHOLDER_MACHINING, String.class));
    }

    @Override
    public int getTitleRes() {
        return R.string.nav_machining;
    }

    private void setStatusText(Status status) {
        final View view = getView();
        if (view == null) {
            return;
        }
        TextView text = view.findViewById(R.id.machining_status);
        text.setText(status == null ? getString(R.string.unknown) : status.getUiValue());
    }

    private void setKeyholderText(String keyholderText) {
        if (keyholderText == null) {
            keyholderText = getString(R.string.unknown);
        } else if (keyholderText.isEmpty()) {
            keyholderText = getString(R.string.keyholder_no_one);
        }
        final View view = getView();
        if (view == null) {
            return;
        }
        ((TextView) view.findViewById(R.id.machining_keyholder)).setText(keyholderText);
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi,
                                boolean isInMainframeWifi, boolean hasMachiningBssid, boolean requireMainframeWifi) {
        if (requireMainframeWifi) {
            doorButtons.setEnabled(isInMainframeWifi && hasMachiningBssid);
        } else {
            doorButtons.setEnabled(true);
        }
    }

    @Override
    public void onNewMsg(Topic topic, Object msg) {
        if (topic == Topic.STATUS_MACHINING) {
            setStatusText((Status) msg);
            return;
        }
        if (topic == Topic.KEYHOLDER_MACHINING) {
            setKeyholderText((String) msg);
        }
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
