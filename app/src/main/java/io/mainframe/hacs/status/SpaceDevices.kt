package io.mainframe.hacs.status

import org.json.JSONException
import org.json.JSONObject
import org.pmw.tinylog.Logger

/**
 * Created by holger on 22.10.17.
 */
data class SpaceDevices(
    val anonPeople: Int,
    val unknownDevices: Int,
    val users: List<User>,
)

data class User(val name: String, val devices: List<Device> = emptyList())
data class Device(val name: String, val location: String)

fun SpaceDevices(rawDataStr: String): SpaceDevices {
    // parses the format from the mqtt:
    // {"deviceCount":36,"unknownDevicesCount":0,"peopleCount":3,"people":["h4uke^2","Pluto","Holger"]}
    // OR (new format)
    // {"people":[{"name":"Domse","devices":null},{"name":"Holger","devices":[{"name":"Handy","location":"Space"},{"name":"Mac","location":"Space"}]},{"name":"MarvinGS: Handy","devices":null},{"name":"MarvinGS: Notebook","devices":null},{"name":"Pluto","devices":null},{"name":"Sascha","devices":null},{"name":"h4uke","devices":null},{"name":"h4uke^2","devices":null},{"name":"larsh404","devices":null},{"name":"larsho","devices":null},{"name":"sre","devices":null}],"peopleCount":11,"deviceCount":29,"unknownDevicesCount":4}
    try {
        val json = JSONObject(rawDataStr)
        val unknownDevices = json.getInt("unknownDevicesCount")
        val people = json.getJSONArray("people")
        val userList = mutableListOf<User>()
        for (i in 0 until people.length()) {
            try {
                val person = people.getJSONObject(i)
                val name = person.getString("name")
                if (person.isNull("devices")) {
                    userList.add(User(name))
                } else {
                    val devices = person.getJSONArray("devices")
                    val deviceList = ArrayList<Device>()
                    for (x in 0 until devices.length()) {
                        val device = devices.getJSONObject(x)
                        val deviceName = device.getString("name")
                        val deviceLocation = device.getString("location")
                        if (deviceName.isNotEmpty()) {
                            deviceList.add(Device(deviceName, deviceLocation))
                        }
                    }
                    userList.add(User(name, deviceList))
                }
            } catch (e: JSONException) {
                // old format
                userList.add(User(people.getString(i)))
            }
        }
        val peopleCount = json.getInt("peopleCount")
        val anonPeople = peopleCount - userList.size

        return SpaceDevices(anonPeople, unknownDevices, userList)
    } catch (e: JSONException) {
        Logger.error(e, "Can't parse the raw devices data: $rawDataStr")
        return SpaceDevices(0, 0, emptyList())
    }
}
