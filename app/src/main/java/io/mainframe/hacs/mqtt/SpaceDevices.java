package io.mainframe.hacs.mqtt;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by holger on 22.10.17.
 */

public class SpaceDevices {

    private static final String TAG = SpaceDevices.class.getName();

    private int anonPeople;
    private int unknownDevices;
    private List<String> users = new ArrayList<>();


    // parses the format from the mqtt:
    // {"deviceCount":36,"unknownDevicesCount":0,"peopleCount":3,"people":["h4uke^2","Pluto","Holger"]}
    public SpaceDevices(String rawDataStr) {
        try {
            JSONObject json = new JSONObject(rawDataStr);
            unknownDevices = json.getInt("unknownDevicesCount");
            final JSONArray people = json.getJSONArray("people");
            for (int i = 0; i < people.length(); i++) {
                users.add(people.getString(i));
            }
            int peopleCount = json.getInt("peopleCount");
            anonPeople = peopleCount - users.size();
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse the raw devices data: " + rawDataStr, e);
        }
    }

    public int getAnonPeople() {
        return anonPeople;
    }

    public int getUnknownDevices() {
        return unknownDevices;
    }

    public List<String> getUsers() {
        return users;
    }
}
