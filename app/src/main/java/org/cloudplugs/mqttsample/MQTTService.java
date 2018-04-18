package org.cloudplugs.mqttsample;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

import org.cloudplugs.mqttsample.activities.FeedActivity;
import org.cloudplugs.mqttsample.model.MessageModel;
import org.cloudplugs.mqttsample.model.OrmliteHelper;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class MQTTService extends Service {
    public static final String MQTT_CONNECT = "MQTT_CONNECT";
    public static final String MQTT_DISCONNECT = "MQTT_DISCONNECT";

    public static final String MQTT_CONNECTED = "MQTT_CONNECTED";
    public static final String MQTT_DISCONNECTED = "MQTT_DISCONNECTED";
    public static final String MQTT_CONNECTION_ERROR = "MQTT_CONNECTION_ERROR";

    public static final String MQTT_MESSAGE_ARRIVED = "MQTT_MESSAGE_ARRIVED";
    public static final String MQTT_MESSAGE_SENT = "MQTT_MESSAGE_SENT";
    public static final String MQTT_PUBLISH = "MQTT_PUBLISH";

    private static final String host = "api.cloudplugs.com";
    private String subscribeChannel = "+/data/topictest";
    private String publishChannel = "/data/topictest";


    private static final String TAG = "MQTTService";
    private static final int MAX_MESSAGES = 3;
    private static boolean hasWifi = false;
    private static boolean hasMmobile = false;
    private Thread thread;
    private ConnectivityManager mConnMan;
    private volatile IMqttAsyncClient mqttClient;
    private String deviceId;


    private String username = null;
    private String password = null;

    private Boolean connect = false;

    public void changedConnectionState() {
        IMqttToken token;
        boolean hasConnectivity;
        boolean hasChanged = false;
        NetworkInfo infos[] = mConnMan.getAllNetworkInfo();

        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getTypeName().equalsIgnoreCase("MOBILE")) {
                if ((infos[i].isConnected() != hasMmobile)) {
                    hasChanged = true;
                    hasMmobile = infos[i].isConnected();
                }
                Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
            } else if (infos[i].getTypeName().equalsIgnoreCase("WIFI")) {
                if ((infos[i].isConnected() != hasWifi)) {
                    hasChanged = true;
                    hasWifi = infos[i].isConnected();
                }
                Log.d(TAG, infos[i].getTypeName() + " is " + infos[i].isConnected());
            }
        }

        hasConnectivity = hasMmobile || hasWifi;
        Log.v(TAG, "hasConn: " + hasConnectivity + " hasChange: " + hasChanged + " - " + (mqttClient == null || !mqttClient.isConnected()));
        if (hasConnectivity && (mqttClient == null || !mqttClient.isConnected())) {
            Log.d(TAG, "doConnect()");
            doConnect();
        } else if (!hasConnectivity && mqttClient != null && mqttClient.isConnected()) {
            Log.d(TAG, "doDisconnect()");
            try {
                token = mqttClient.disconnect();
                token.waitForCompletion(1000);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    class MQTTBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "received intent");
            //if (connect) changedConnectionState();
        }
    }

    ;

    public class MQTTBinder extends Binder {
        public MQTTService getService() {
            return MQTTService.this;
        }
    }

    @Override
    public void onCreate() {
        /* binding for connectivity change */

        Log.v(TAG, "called onCreate");
        IntentFilter intentf = new IntentFilter();
        setClientID();
        intentf.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new MQTTBroadcastReceiver(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged()");
        android.os.Debug.waitForDebugger();
        super.onConfigurationChanged(newConfig);

    }

    private void setClientID() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        deviceId = wInfo.getMacAddress();
        if (deviceId == null) {
            deviceId = MqttAsyncClient.generateClientId();
        }
    }

    private void doConnect() {
        Log.d(TAG, "doConnect()");
        IMqttToken token;
        MqttConnectOptions options = new MqttConnectOptions();
        if (username != null && password != null) {
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setKeepAliveInterval(450);
        }
        options.setCleanSession(true);
        try {
            mqttClient = new MqttAsyncClient("tcp://" + host + ":1883", deviceId, new MemoryPersistence());
            token = mqttClient.connect(options);
            token.waitForCompletion(15000);
            Log.v(TAG, "connected");
            mqttClient.setCallback(new MqttEventCallback());
            token = mqttClient.subscribe(subscribeChannel, 1);
            token.waitForCompletion(15000);
            Log.v(TAG, "subscribed");

            Intent intent = new Intent(MQTT_CONNECTED);
            sendBroadcast(intent);

            changedConnectionState();
        } catch (MqttSecurityException e) {
            Intent intent = new Intent();
            intent.setAction(MQTT_CONNECTION_ERROR);
            intent.putExtra("reason", e.getMessage());
            sendBroadcast(intent);
        } catch (MqttException e) {
            Log.v(TAG, "" + e.getReasonCode());
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_BROKER_UNAVAILABLE:
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                case MqttException.REASON_CODE_CONNECTION_LOST:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    Log.v(TAG, "c" + e.getMessage());
                    e.printStackTrace();
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    Intent i = new Intent("RAISEALLARM");
                    i.putExtra("ALLARM", e);
                    Log.e(TAG, "b" + e.getMessage());
                    break;
                default:
                    Log.e(TAG, "a" + e.getMessage());
            }

            Intent intent = new Intent(MQTT_CONNECTION_ERROR);
            intent.putExtra("reason", e.getMessage());
            sendBroadcast(intent);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onSartCommand");

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case MQTT_CONNECT:
                    username = intent.getStringExtra("username");
                    password = intent.getStringExtra("password");
                    doConnect();
                    break;
                case MQTT_PUBLISH:
                    Bundle extra = intent.getExtras();
                    String received = extra.getString("publish");
                    Log.v(TAG, "sent " + received);

                    MqttMessage mqttMessage = new MqttMessage();

                    mqttMessage.setQos(1);
                    mqttMessage.setRetained(false);

                    try {
                        if (mqttMessage != null) {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("data", received);

                            mqttMessage.setPayload(jsonObject.toString().getBytes());

                            mqttClient.publish(publishChannel, mqttMessage);

                            Log.v(TAG, "publishing "+mqttMessage.toString());
                            Log.v(TAG, "on channel "+publishChannel);
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case MQTT_DISCONNECT:
                    if (mqttClient != null) try {
                        mqttClient.disconnect();
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
        return START_STICKY;
    }

    private class MqttEventCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable arg0) {
            Intent intent = new Intent(MQTT_DISCONNECTED);
            sendBroadcast(intent);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        @SuppressLint("NewApi")
        public void messageArrived(String topic, final MqttMessage msg) throws Exception {
            Log.i(TAG, "Message arrived from topic" + topic);
            Handler h = new Handler(getMainLooper());
            h.post(new Runnable() {
                @Override
                public void run() {
                    String stringMsg = new String(msg.getPayload());

                    Log.v(TAG, "message received " + stringMsg);

                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(stringMsg);
                        String msg = jsonObject.getString("data");

                        notifyMessage(msg);
                        pushMessage(msg);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void notifyMessage(String stringMsg) {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
        String currentActivity = taskInfo.get(0).topActivity.getClassName();
        Log.v(TAG, "currentActivity : #"+currentActivity+"#");
        if (!currentActivity.equals("org.cloudplugs.mqttsample.activities.FeedActivity")) {
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            componentInfo.getPackageName();

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setSmallIcon(R.drawable.ic_action_name)
                            .setContentTitle("MQTT Message Arrived")
                            .setContentText(stringMsg);
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(getApplicationContext(), FeedActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getApplicationContext());
            stackBuilder.addParentStack(FeedActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }

        Intent i = new Intent(MQTT_MESSAGE_ARRIVED);
        i.putExtra("data", stringMsg);
        sendBroadcast(i);
    }

    private void pushMessage(String stringMsg) {
        OrmliteHelper ormliteHelper = OpenHelperManager.getHelper(this,
                OrmliteHelper.class);

        try {
            Dao<MessageModel, Long> todoDao = ormliteHelper.getDao();
            Date currDateTime = new Date(System.currentTimeMillis());
            todoDao.create(new MessageModel(stringMsg, currDateTime));
        } catch (SQLException e) {
            Log.v(TAG, e.getMessage());
        }
    }



    public String getThread() {
        return Long.valueOf(thread.getId()).toString();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind called");
        return null;
    }


}
