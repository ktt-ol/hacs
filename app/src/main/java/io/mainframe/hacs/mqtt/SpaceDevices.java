package io.mainframe.hacs.mqtt;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by holger on 22.10.17.
 */

public class SpaceDevices {

    private int anonPeople;
    private int unknownDevices;
    private List<User> userList = new ArrayList<>();


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
                        userList.add(new User(name));
                    } else {

                        JSONArray devices = person.getJSONArray("devices");
                        ArrayList<Device> deviceList = new ArrayList<>();
                        if (devices != null) {
                            for (int x = 0; x < devices.length(); x++) {
                                JSONObject device = devices.getJSONObject(x);
                                String deviceName = device.getString("name");
                                String deviceLocation = device.getString("location");
                                if (deviceName != null && !deviceName.isEmpty()) {
                                    deviceList.add(new Device(deviceName, deviceLocation));
                                }
                            }
                        }
                        userList.add(new User(name, deviceList));
                    }
                } catch (JSONException e) {
                    // old format
                    userList.add(new User(people.getString(i)));
                }
            }
            int peopleCount = json.getInt("peopleCount");
            anonPeople = peopleCount - userList.size();
        } catch (JSONException e) {
            Logger.error("Can't parse the raw devices data: " + rawDataStr, e);
        }
    }

    public int getAnonPeople() {
        return anonPeople;
    }

    public int getUnknownDevices() {
        return unknownDevices;
    }

    public List<User> getUsers() {
        return userList;
    }

    public static class User {
        private String name;
        private List<Device> devices;

        User(String name) {
            this.name = name;
            this.devices = Collections.emptyList();
        }

        User(String name, List<Device> devices) {
            this.name = name;
            this.devices = devices;
        }

        public String getName() {
            return name;
        }

        public List<Device> getDevices() {
            return devices;
        }
    }

    public static class Device {
        private String name;
        private String location;

        Device(String name, String location) {
            this.name = name;
            this.location = location;
        }

        public String getName() {
            return name;
        }

        public String getLocation() {
            return location;
        }
    }
}
