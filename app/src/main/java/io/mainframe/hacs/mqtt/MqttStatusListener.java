package io.mainframe.hacs.mqtt;

import io.mainframe.hacs.common.Constants;

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
        STATUS(Constants.MQTT_TOPIC_STATUS),
        STATUS_NEXT(Constants.MQTT_TOPIC_STATUS_NEXT),
        KEYHOLDER(Constants.MQTT_TOPIC_KEYHOLDER),
        DEVICES(Constants.MQTT_TOPIC_DEVICES);

        private final String value;

        Topic(String value) {
            this.value = value;
        }

        public static Topic byValue(String value) {
            for (Topic topic : values()) {
                if (topic.value.equals(value)) {
                    return topic;
                }
            }

            throw new IllegalArgumentException("Unsupported value: " + value);
        }
    }
}
