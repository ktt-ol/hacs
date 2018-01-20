package io.mainframe.hacs.PageFragments;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.mainframe.hacs.R;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.mqtt.SpaceDevices;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;

/**
 */
public class OverviewFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener, MqttStatusListener {

    private LocationColor locationColor = new LocationColor();

    public OverviewFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_overview, container, false);

        view.findViewById(R.id.overview_change).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().navigateToPage(StatusFragment.class);
            }
        });
        view.findViewById(R.id.overview_buzzer_outer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().sendSshCommand(DoorCommand.getOuterDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_buzzer_inner_glass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().sendSshCommand(DoorCommand.getInnerGlassDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_buzzer_inner_metal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().sendSshCommand(DoorCommand.getInnerGlassDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_become_keyholder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: there should be an extra command to become keyholder
                final Status lastStatus = getInteraction().getMqttConnector().getLastStatus();
                if (lastStatus != null) {
                    getInteraction().sendSshCommand(DoorCommand.getSwitchDoorStateCmd(lastStatus));
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        PkCredentials credentials = new PkCredentials(PreferenceManager.getDefaultSharedPreferences(getActivity()));
        boolean readOnlyMode = !credentials.isPasswordSet();

        if (!readOnlyMode) {
            final NetworkStatus networkStatus = getInteraction().getNetworkStatus();
            networkStatus.addListener(this);
            setButtonsEnabled(networkStatus.isInMainframeWifi());
        } else {
            setButtonsEnabled(false);
        }

        final MqttConnector mqtt = getInteraction().getMqttConnector();
        mqtt.addListener(this, EnumSet.of(Topic.STATUS, Topic.KEYHOLDER, Topic.DEVICES));

        setStatusText(mqtt.getLastStatus());
        setKeyholderText(mqtt.getLastKeyholder());
        setDevicesText(mqtt.getLastDevices());
    }

    @Override
    public void onPause() {
        super.onPause();

        getInteraction().getMqttConnector().removeAllListener(this);
        getInteraction().getNetworkStatus().removeListener(this);
    }

    @Override
    public int getTitleRes() {
        return R.string.nav_overview;
    }


    private void setStatusText(Status status) {
        TextView text = (TextView) getView().findViewById(R.id.overview_status);
        text.setText(status == null ? getString(R.string.unknown) : status.getUiValue());
    }

    private void setKeyholderText(String keyholderText) {
        if (keyholderText == null) {
            keyholderText = getString(R.string.unknown);
        } else if (keyholderText.isEmpty()) {
            keyholderText = getString(R.string.keyholder_no_one);
        }
        ((TextView) getView().findViewById(R.id.overview_keyholder)).setText(keyholderText);
    }

    private void setDevicesText(SpaceDevices devices) {
        String formatted = "?";
        if (devices != null) {
            StringBuilder buffer = new StringBuilder();
            for (SpaceDevices.User user : devices.getUsers()) {
                buffer.append("● ").append(user.getName());
                if (!user.getDevices().isEmpty()) {
                    buffer.append(" [");

                    List<SpaceDevices.Device> userDevices = user.getDevices();
                    for (int i = 0; i < userDevices.size(); i++) {
                        SpaceDevices.Device device = userDevices.get(i);
                        String color = locationColor.getColor(device.getLocation());
                        if (color != null) {
                            buffer.append(makeColorTag(device.getName(), color));
                        } else {
                            buffer.append(device.getName());
                        }

                        if (i < userDevices.size() - 1) {
                            buffer.append(", ");
                        }
                    }
                    buffer.append("]");
                }
                buffer.append("<br>\n");
            }
            buffer.append("<br>\nOrte: ");
            for (Pair<String, String> colorPair : locationColor.getAll()) {
                buffer.append(makeColorTag(colorPair.first, colorPair.second)).append(" ");
            }

            buffer.append("<br>\n<br>\n");
            buffer.append(getString(R.string.overview_anonPerson)).append(": ").append(devices.getAnonPeople()).append("<br>\n")
                    .append(getString(R.string.overview_unknownDev)).append(": ").append(devices.getUnknownDevices());

            buffer.append("<br><br><br><br>");
            formatted = buffer.toString();
        }
        ((TextView) getView().findViewById(R.id.overview_devices)).setText(Html.fromHtml(formatted));
    }

    private String makeColorTag(String content, String color) {
        return "<font color='" + color + "'>" + content + "</font>";
    }

    private void setButtonsEnabled(boolean enabled) {
        getView().findViewById(R.id.overview_buzzer_outer).setEnabled(enabled);
        getView().findViewById(R.id.overview_buzzer_inner_metal).setEnabled(enabled);
        getView().findViewById(R.id.overview_become_keyholder).setEnabled(enabled);
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi, boolean isInMainframeWifi) {
        setButtonsEnabled(isInMainframeWifi);
    }

    @Override
    public void onNewMsg(Topic topic, Object msg) {
        if (topic == Topic.STATUS) {
            setStatusText((Status) msg);
        } else if (topic == Topic.KEYHOLDER) {
            setKeyholderText((String) msg);
        } else if (topic == Topic.DEVICES) {
            setDevicesText((SpaceDevices) msg);
        }
    }

    @Override
    public void onMqttConnected() {
        final MqttConnector mqtt = getInteraction().getMqttConnector();
        setStatusText(mqtt.getLastStatus());
        setKeyholderText(mqtt.getLastKeyholder());
        setDevicesText(mqtt.getLastDevices());
    }

    @Override
    public void onMqttConnectionLost() {
        setStatusText(null);
        setKeyholderText(null);
        setDevicesText(null);
    }
}
