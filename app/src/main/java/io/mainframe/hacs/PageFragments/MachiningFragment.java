package io.mainframe.hacs.PageFragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Objects;

import io.mainframe.hacs.R;
import io.mainframe.hacs.components.DoorButtons;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.ssh.DoorCommand;
import io.mainframe.hacs.ssh.PkCredentials;
import io.mainframe.hacs.status.StatusEvent;
import io.mainframe.hacs.status.SpaceStatusService;
import io.mainframe.hacs.status.Subscription;

import static io.mainframe.hacs.common.Constants.MACHINING_DOOR;

/**
 * Created by holger on 21.05.18.
 */

public class MachiningFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener {

    private DoorButtons doorButtons;
    // if the mqtt password is not set
    private boolean readOnlyMode = false;
    private Subscription subscription;

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

        SpaceStatusService statusService = getInteraction().getStatusService();
        this.subscription = statusService.subscribe((event, value) -> {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                if (Objects.requireNonNull(event) == StatusEvent.STATUS_MACHINING) {
                    setStatusText(Status.byEventStatusValue(value));
                }
            });
            return null;
        });
        setStatusText(statusService.getLastStatusValue(StatusEvent.STATUS_MACHINING));

        // Not supported by Status
//        setKeyholderText(statusService.getLastValue(Event.KEYHOLDER_MACHINING));
        setKeyholderText(null);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (subscription != null) {
            subscription.unsubscribe();
            subscription = null;
        }
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
}
