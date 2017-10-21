package io.mainframe.hacs.PageFragments;


import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import io.mainframe.hacs.R;
import io.mainframe.hacs.main.NetworkStatus;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttConnector;
import io.mainframe.hacs.mqtt.MqttStatusListener;
import io.mainframe.hacs.ssh.PkCredentials;

/**
 * A simple {@link Fragment} subclass.
 */
public class OverviewFragment extends BasePageFragment implements NetworkStatus.NetworkStatusListener, MqttStatusListener {


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
        view.findViewById(R.id.overview_buzzer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // buzz
            }
        });
        view.findViewById(R.id.overview_become_keyholder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // keyholder
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
        mqtt.addListener(this);

        setStatusText(mqtt.getLastStatus());
    }

    @Override
    public void onPause() {
        super.onPause();

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

    private void setButtonsEnabled(boolean enabled) {
        getView().findViewById(R.id.overview_buzzer).setEnabled(enabled);
        getView().findViewById(R.id.overview_become_keyholder).setEnabled(enabled);
    }

    /* callback */

    @Override
    public void onNetworkChange(boolean hasNetwork, boolean hasMobile, boolean hasWifi, boolean isInMainframeWifi) {
        setButtonsEnabled(isInMainframeWifi);
    }

    @Override
    public void onNewStatus(Topic topic, Status newStatus) {
        if (topic != Topic.STATUS) {
            return;
        }
        setStatusText(newStatus);
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
