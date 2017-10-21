package io.mainframe.hacs.mqtt;

import io.mainframe.hacs.common.Constants;
import io.mainframe.hacs.main.Status;

/**
 * Created by holger on 13.08.16.
 */
public interface MqttStatusListener {

    enum Topic {
        STATUS(Constants.MQTT_TOPIC_STATUS),
        STATUS_NEXT(Constants.MQTT_TOPIC_STATUS_NEXT),
        KEYHOLDER(Constants.MQTT_TOPIC_KEYHOLDER);

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

    void onNewStatus(Topic topic, Status newStatus);

    void onNewKeyHolder(String keyholder);

    void onMqttConnected();

//    void onMqttMessage(String topic, String msg);

    void onMqttConnectionLost();
}
