package io.mainframe.hacs.mqtt;

/**
 * Created by holger on 13.08.16.
 */
public interface MqttConnectorCallbacks {

    /**
     * The password used to connect to the mqtt server.
     * @return return an empty if the connection is done without user/password.
     */
    String getConnectionPassword();

    void onMqttReady();

    void onMqttMessage(String topic, String msg);

    void error(String msg);
}
