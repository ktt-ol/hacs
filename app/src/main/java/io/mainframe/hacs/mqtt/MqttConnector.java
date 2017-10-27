package io.mainframe.hacs.mqtt;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import io.mainframe.hacs.common.Constants;
import io.mainframe.hacs.main.Status;
import io.mainframe.hacs.mqtt.MqttStatusListener.Topic;

/**
 * Responsible for the conneciton to the client server. The connection will be initialized automatically and ....
 */
public class MqttConnector {

    private static final String PREF_MQTT_PASSWORD = "mqttPassword";
    private static final String TAG = MqttConnector.class.getName();

    private final Context ctx;
    private final SharedPreferences prefs;

    private final List<Listener> allListener = Collections.synchronizedList(new ArrayList<Listener>());

    private MqttAndroidClient client;
    private boolean isPassowrdSet = false;

    // null means "unknown" - if the client is disconnected the last state will be reset to unknown!
    private Status lastStatus = null;
    private Status lastStatusNext = null;
    private String lastKeyholder = null;
    private SpaceDevices lastDevices = null;

    public MqttConnector(Context ctx, SharedPreferences prefs) {
        this.ctx = ctx;
        this.prefs = prefs;
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
                Log.i(TAG, "Lost connection: " + (cause == null ? "/" : cause.getMessage()));
                handleError("Lost connection.", cause);
                lastStatus = null;
                lastStatusNext = null;
                for (Listener listener : MqttConnector.this.allListener) {
                    listener.callbacks.onMqttConnectionLost();
                }
            }

            @Override
            public void messageArrived(String topicStr, MqttMessage message) throws Exception {
                String strMsg = message.toString();
                Log.d(TAG, "Got mqtt msg (" + topicStr + "): " + strMsg);
                Topic topic = Topic.byValue(topicStr);

                Object msgValue = null;
                switch (topic) {
                    case STATUS:
                        msgValue = lastStatus = Status.byMqttValue(strMsg);
                        break;
                    case STATUS_NEXT:
                        msgValue = lastStatusNext = Status.byMqttValue(strMsg);
                        break;
                    case KEYHOLDER:
                        msgValue = lastKeyholder = strMsg;
                        break;
                    case DEVICES:
                        msgValue = lastDevices = new SpaceDevices(strMsg);
                        break;
                }

                for (Listener listener : allListener) {
                    if (listener.topics.contains(topic)) {
                        listener.callbacks.onNewMsg(topic, msgValue);
                    }
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("message send");
            }
        });
    }

    public Status getLastStatus() {
        return lastStatus;
    }

    public boolean isPasswordSet() {
        return isPassowrdSet;
    }

    public Status getLastNextStatus() {
        return lastStatusNext;
    }

    public String getLastKeyholder() {
        return lastKeyholder;
    }

    public SpaceDevices getLastDevices() {
        return lastDevices;
    }

    public void addListener(MqttStatusListener listener, EnumSet<Topic> topics) {
        allListener.add(new Listener(topics, listener));
    }

    /* Listener */

    public void removeAllListener(MqttStatusListener listener) {
        final Iterator<Listener> iter = allListener.iterator();
        while (iter.hasNext()) {
            if (iter.next().callbacks == listener) {
                iter.remove();
                return;
            }
        }
    }

    private boolean hasListener() {
        return !allListener.isEmpty();
    }

    public void disconnect() {
        try {
            this.client.close();
        } catch (Exception e) {
            Log.e(TAG, "Error during close", e);
        }

        this.client.unregisterResources();
        this.client = null;
    }

    /* --- */

    public void connect() {
        if (client != null) {
            disconnect();
        }

        init();

        Log.d(TAG, "Try to connect.");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        // reconnect doesn't work good enough (and needs cleanSession = false)
        options.setAutomaticReconnect(false);

        String password = prefs.getString(PREF_MQTT_PASSWORD, "");
        isPassowrdSet = !password.isEmpty();
        if (isPassowrdSet) {
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

                // the not set value is an empty string that is not shown in mqtt
                // we have now a valid connection, thus we set not_set as default
                lastStatusNext = Status.NOT_SET;
                // same here
                lastKeyholder = "";

                for (Listener listener : MqttConnector.this.allListener) {
                    listener.callbacks.onMqttConnected();
                }

                subscribe(Constants.MQTT_TOPIC_STATUS);
                subscribe(Constants.MQTT_TOPIC_STATUS_NEXT);
                subscribe(Constants.MQTT_TOPIC_KEYHOLDER);
                subscribe(Constants.MQTT_TOPIC_DEVICES);
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

    /**
     * @param msg
     * @param excp can be null
     */
    private void handleError(String msg, Throwable excp) {
        String msgWithExcp = msg;
        if (excp != null) {
            msgWithExcp += " (" + excp.getMessage() + ")";
        }
        Log.e(TAG, msgWithExcp, excp);
    }

    private static class Listener {
        private EnumSet<Topic> topics;
        private MqttStatusListener callbacks;

        Listener(EnumSet<Topic> topics, MqttStatusListener callbacks) {
            this.topics = topics;
            this.callbacks = callbacks;
        }
    }
}
