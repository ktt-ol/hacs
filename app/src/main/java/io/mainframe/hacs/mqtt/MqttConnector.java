package io.mainframe.hacs.mqtt;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.IOException;
import java.io.InputStream;

import io.mainframe.hacs.Constants;
import io.mainframe.hacs.NetworkStatus;

/**
 * Responsible for the conneciton to the client server. The connection will be initialized automatically and ....
 */
public class MqttConnector {

    private static final String TAG = MqttConnector.class.getName();

    private final Context ctx;
    private final MqttConnectorCallbacks callbacks;

    private MqttAndroidClient client;

    public MqttConnector(Context ctx, MqttConnectorCallbacks callbacks) {
        this.ctx = ctx;
        this.callbacks = callbacks;

        init();
    }

    private void init() {
        this.client = new MqttAndroidClient(this.ctx,
                Constants.MQTT_SERVER,
                MqttClient.generateClientId(),
                MqttAndroidClient.Ack.AUTO_ACK
        );
        this.client.setTraceEnabled(true);
        this.client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.i(TAG, "Lost connection: " + cause.getMessage());
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if (NetworkStatus.hasNetwork()) {
//                    try {
//                        connect();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }

                handleError("Lost connection.", cause);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String strMsg = message.toString();
                Log.d(TAG, "Got mqtt msg (" + topic + "): " + strMsg);
                MqttConnector.this.callbacks.onMqttMessage(topic, strMsg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("message send");
            }
        });

        connect();
    }

    public void disconnect() {
        if (!this.client.isConnected()) {
            Log.d(TAG, "Client is already disconnected.");
            this.client.unregisterResources();
            return;
        }

        try {
            this.client.disconnect();
            this.client.unregisterResources();
        } catch (MqttException e) {
            Log.e(TAG, "Error during disconnect", e);
        }
    }

    public void connect() {
        if (client.isConnected()) {
            Log.d(TAG, "Client is already connected.");
            return;
        }

        Log.d(TAG, "Try to connect.");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);

        String password = this.callbacks.getConnectionPassword();
        if (!password.isEmpty()) {
            Log.d(TAG, "Using password to connect");
            options.setUserName(Constants.MQTT_USER);
            options.setPassword(password.toCharArray());
        }
        final IMqttToken token;
        try {
            InputStream input = this.ctx.getAssets().open(Constants.KEYSTORE_FILE);
            options.setSocketFactory(client.getSSLSocketFactory(input, Constants.KEYSTORE_PW));
            token = client.connect(options);
        } catch (Exception e) {
            handleError("Can't connect to mqqt server.", e);
            return;
        }

        token.setActionCallback(new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                // We are connected
                Log.d(TAG, "connect onSuccess");
                MqttConnector.this.callbacks.onMqttReady();

                subscribe(Constants.MQTT_TOPIC_STATUS);
                subscribe(Constants.MQTT_TOPIC_STATUS_NEXT);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                // Something went wrong e.g. connection timeout or firewall problems
                handleError("Can't connect to mqtt server.", exception);
            }
        });
    }

    public void send(String topic, String msg) {
        Log.i(TAG, "Sending '" + msg + "' on " + topic);
        try {
            client.publish(topic, new MqttMessage(msg.getBytes()));
        } catch (MqttException e) {
            handleError("Can't publish message.", e);
        }
    }

    private void subscribe(final String topic) {
        try {
            client.subscribe(topic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Subscribed on topic " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    handleError("Subscription failure for topic '" + topic + "'.", exception);
                }
            });
        } catch (MqttException e) {
            handleError("Can't subscribe to " + topic, e);
        }
    }

    private void handleError(String msg, Throwable excp) {
        String msgWithExcp = msg + " (" + excp.getMessage() + ")";
        Log.e(TAG, msgWithExcp, excp);
        disconnect();
        MqttConnector.this.callbacks.error(msgWithExcp);
    }
}
