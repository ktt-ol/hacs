package io.mainframe.hacs.PageFragments;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.EnumSet;

import io.mainframe.hacs.R;
import io.mainframe.hacs.common.YesNoDialog;
import io.mainframe.hacs.components.DoorButtons;
import io.mainframe.hacs.main.BackDoorStatus;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;

import static io.mainframe.hacs.common.Constants.SPACE_DOOR;

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
            public void onClick(final DoorButtons.DoorButton doorButton, View view) {
                if (doorButton.getStatus() == Status.CLOSE) {
                    final BackDoorStatus backDoorStatus = getInteraction().getMqttConnector().getLastValue(Topic.BACK_DOOR_BOLT, BackDoorStatus.class);
                    if (backDoorStatus == BackDoorStatus.OPEN) {
                        YesNoDialog.show(getContext(), "Back-Door prüfen.", "Die Back-Door ist noch offen. Wirklich abschließen?", "bd", new YesNoDialog.ResultListener() {
                            @Override
                            public void dialogClosed(String tag, boolean resultOk) {
                                if (resultOk) {
                                    getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getSwitchDoorStateCmd(doorButton.getStatus()));
                                }
                            }
                        });

                        return;
                    }
                }

                getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getSwitchDoorStateCmd(doorButton.getStatus()));
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
            doorButtons.setEnabled(networkStatus.isInMainframeWifi() && !networkStatus.hasMachiningBssid());
        } else {
            doorButtons.setEnabled(false);
        }

        final MqttConnector mqtt = getInteraction().getMqttConnector();
        mqtt.addListener(this, EnumSet.of(Topic.STATUS, Topic.BACK_DOOR_BOLT));

        setStatusText(mqtt.getLastValue(Topic.STATUS, Status.class));
        setLedImage(mqtt.getLastValue(Topic.BACK_DOOR_BOLT, BackDoorStatus.class));
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
        TextView text = getView().findViewById(R.id.status_status);
        text.setText(status == null ? getString(R.string.unknown) : status.getUiValue());
    }

    private void setLedImage(BackDoorStatus status) {
        final ImageView imageView = getView().findViewById(R.id.back_door_status);

        switch (status) {
            case OPEN:
                imageView.setImageResource(R.drawable.ic_led_red_black);
                break;
            case CLOSED:
                imageView.setImageResource(R.drawable.ic_led_blue_black);
                break;
            default:
                imageView.setImageResource(R.drawable.ic_button_black);
        }
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi, boolean isInMainframeWifi, boolean hasMachiningBssid) {
        doorButtons.setEnabled(isInMainframeWifi);
    }

    @Override
    public void onNewMsg(Topic topic, Object msg) {
        if (topic == Topic.STATUS) {
            setStatusText((Status) msg);
        } else if (topic == Topic.BACK_DOOR_BOLT) {
            setLedImage((BackDoorStatus) msg);
        }
    }

    @Override
    public void onMqttConnected() {
        // not needed
    }

    @Override
    public void onMqttConnectionLost() {
        setStatusText(null);
        setLedImage(BackDoorStatus.UNKNOWN);
    }
}
