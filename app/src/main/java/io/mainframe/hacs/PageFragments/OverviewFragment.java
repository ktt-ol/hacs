package io.mainframe.hacs.PageFragments;


import android.os.Bundle;
import android.text.Html;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.pmw.tinylog.Logger;

import java.util.List;
import java.util.Objects;

import io.mainframe.hacs.R;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.status.StatusEvent;
import io.mainframe.hacs.status.SpaceDevices;
import io.mainframe.hacs.status.SpaceStatusService;
import io.mainframe.hacs.status.Subscription;

import static io.mainframe.hacs.common.Constants.SPACE_DOOR;

/**
 *
 */
public class OverviewFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener {

    private LocationColor locationColor = new LocationColor();
    private Subscription subscription;

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
                getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getOuterDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_buzzer_inner_glass).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getInnerGlassDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_buzzer_inner_metal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getInnerMetalDoorBuzzerCmd());
            }
        });
        view.findViewById(R.id.overview_become_keyholder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: there should be an extra command to become keyholder
                Status lastStatus = getInteraction().getStatusService().getLastStatusValue(StatusEvent.SPACE_STATUS);
                if (lastStatus != null) {
                    getInteraction().sendSshCommand(SPACE_DOOR, DoorCommand.getSwitchDoorStateCmd(lastStatus));
                }
            }
        });

        final TextView trashInfoText = view.findViewById(R.id.overview_trash_info);
        final String trashInfo = getInteraction().getTrashCalendar().getTrashSummaryForTomorrow();
        if (trashInfo == null) {
            trashInfoText.setVisibility(View.GONE);
        } else {
            trashInfoText.setText(String.format(getString(R.string.overview_trashInfo), trashInfo));
            trashInfoText.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean readOnlyMode = !PkCredentials.isPasswordSet(getContext());
        if (!readOnlyMode) {
            final NetworkStatus networkStatus = getInteraction().getNetworkStatus();
            networkStatus.addListener(this);

            if (networkStatus.isRequireMainframeWifi()) {
                setButtonsEnabled(networkStatus.isInMainframeWifi(),
                        networkStatus.isInMainframeWifi() && !networkStatus.hasMachiningBssid());
            } else {
                setButtonsEnabled(true, true);
            }
        } else {
            setButtonsEnabled(false, false);
        }

        SpaceStatusService statusService = getInteraction().getStatusService();
        this.subscription = statusService.subscribe((event, value) -> {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                switch (event) {
                    case SPACE_STATUS:
                        setStatusText(Status.byEventStatusValue(value));
                        break;
                    case KEYHOLDER:
                        setKeyholderText(value);
                        break;
                    case DEVICES:
                        setDevicesText(new SpaceDevices(value));
                        break;
                }
            });
            return null;
        });

        setStatusText(statusService.getLastStatusValue(StatusEvent.SPACE_STATUS));
        setKeyholderText(statusService.getLastValue(StatusEvent.KEYHOLDER));
        String devices = statusService.getLastValue(StatusEvent.DEVICES);
        if (devices != null) {
            setDevicesText(new SpaceDevices(devices));
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
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

    private void setButtonsEnabled(boolean buzzerEnabled, boolean becomeKeyholderEnabled) {
        getView().findViewById(R.id.overview_buzzer_outer).setEnabled(buzzerEnabled);
        getView().findViewById(R.id.overview_buzzer_inner_glass).setEnabled(buzzerEnabled);
        getView().findViewById(R.id.overview_buzzer_inner_metal).setEnabled(buzzerEnabled);
        getView().findViewById(R.id.overview_become_keyholder).setEnabled(becomeKeyholderEnabled);
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi,
                                boolean isInMainframeWifi, boolean hasMachiningBssid, boolean requireMainframeWifi) {
        if (requireMainframeWifi) {
            setButtonsEnabled(isInMainframeWifi, isInMainframeWifi && !hasMachiningBssid);
        } else {
            setButtonsEnabled(true, true);
        }
    }
}
