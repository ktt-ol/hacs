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
    // OR (new format)
    // {"people":[{"name":"Domse","devices":null},{"name":"Holger","devices":[{"name":"Handy","location":"Space"},{"name":"Mac","location":"Space"}]},{"name":"MarvinGS: Handy","devices":null},{"name":"MarvinGS: Notebook","devices":null},{"name":"Pluto","devices":null},{"name":"Sascha","devices":null},{"name":"h4uke","devices":null},{"name":"h4uke^2","devices":null},{"name":"larsh404","devices":null},{"name":"larsho","devices":null},{"name":"sre","devices":null}],"peopleCount":11,"deviceCount":29,"unknownDevicesCount":4}
    public SpaceDevices(String rawDataStr) {
        try {
            JSONObject json = new JSONObject(rawDataStr);
            unknownDevices = json.getInt("unknownDevicesCount");
            final JSONArray people = json.getJSONArray("people");
            for (int i = 0; i < people.length(); i++) {
                try {
                    JSONObject person = people.getJSONObject(i);
                    String name = person.getString("name");
                    if (person.isNull("devices")) {
                        users.add(name);
                    } else {

                        JSONArray devices = person.getJSONArray("devices");
                        ArrayList<String> devicesNames = new ArrayList<>();
                        if (devices != null) {
                            for (int x = 0; x < devices.length(); x++) {
                                JSONObject device = devices.getJSONObject(x);
                                String deviceName = device.getString("name");
                                if (deviceName != null && !deviceName.isEmpty()) {
                                    devicesNames.add(deviceName);
                                }
                            }
                        }
                        users.add(name + " " + devicesNames.toString());
                    }
                } catch (JSONException e) {
                    // old format
                    users.add(people.getString(i));
                }
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
