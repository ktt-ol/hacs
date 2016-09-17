package io.mainframe.hacs.mqtt;

/**
 * Created by holger on 13.08.16.
 */
public interface MqttConnectorCallbacks {
    void onMqttReady();

    void onMqttMessage(String topic, String msg);

    void error(String msg);
}
