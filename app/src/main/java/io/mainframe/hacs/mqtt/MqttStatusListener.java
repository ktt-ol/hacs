package io.mainframe.hacs.mqtt;

import io.mainframe.hacs.common.Constants;
import io.mainframe.hacs.main.BackDoorStatus;
import io.mainframe.hacs.main.Status;

/**
 * Created by holger on 13.08.16.
 */
public interface MqttStatusListener {

    /**
     * The type of the msg depends on the topic.
     * @param topic
     * @param msg
     */
    void onNewMsg(Topic topic, Object msg);

    void onMqttConnected();

    void onMqttConnectionLost();

    enum Topic {

        STATUS(Constants.MQTT_TOPIC_STATUS, Status.NOT_SET),
        STATUS_NEXT(Constants.MQTT_TOPIC_STATUS_NEXT, Status.NOT_SET),
        KEYHOLDER(Constants.MQTT_TOPIC_KEYHOLDER, ""),
        DEVICES(Constants.MQTT_TOPIC_DEVICES, null),
        // -1 unknown / 0 door open / 1 door closed
        BACK_DOOR_BOLT(Constants.MQTT_TOPIC_BACK_DOOR_BOLT, BackDoorStatus.UNKNOWN),

        STATUS_MACHINING(Constants.MQTT_TOPIC_MACHINING_STATUS, Status.NOT_SET),
        KEYHOLDER_MACHINING(Constants.MQTT_TOPIC_MACHINING_KEYHOLDER, "");

        private final String name;
        private final Object defaultValue;

        Topic(String name, Object defaultValue) {
            this.name = name;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public Object getDefaultValue() {
            return defaultValue;
        }

        public static Topic byValue(String value) {
            for (Topic topic : values()) {
                if (topic.name.equals(value)) {
                    return topic;
                }
            }

            throw new IllegalArgumentException("Unsupported name: " + value);
        }
    }

}
