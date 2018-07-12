package com.shra1.activemqmqtt.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.shra1.activemqmqtt.MainActivity;
import com.shra1.activemqmqtt.R;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static com.shra1.activemqmqtt.MainActivity.TAG;

public class BGService extends Service {
    public static String clientId;
    MqttAndroidClient mqttAndroidClient;
    Context context;
    private NotificationManager notificationManager;
    private String CHANNEL_ID = "myChannelId";
    private CharSequence CHANNEL_NAME = "shrawansChannel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getApplicationContext();
        try {
            clientId = "AndroidClient";
            mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), MainActivity.serverURI, clientId);

            MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
            mqttAndroidClient.connect(mqttConnectOptions, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess: connection successfull");
                    subscribe();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "onFailure: connection failutre", exception);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }


        return START_STICKY;
    }

    private void subscribe() {
        String subject = "English";
        try {
            mqttAndroidClient.subscribe(subject, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, final MqttMessage message) throws Exception {
                    Log.d(TAG, "messageArrived: ");

                    if (MainActivity.getInstance() != null) {
                        MainActivity.getInstance().showMessageOnTextView(new String(message.getPayload()));
                    } else {
                        showNotification(new String(message.getPayload()));
                    }

                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(2000);


                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String s) {

        if (notificationManager == null) {
            notificationManager =
                    (NotificationManager)
                            context.getSystemService(Context.NOTIFICATION_SERVICE);
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }


        NotificationCompat.Builder builder
                = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MQTT message recieved")
                .setAutoCancel(true)
                //.setContentIntent(pendingIntent)
                .setContentText(s)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT);

        notificationManager.notify(45, builder.build());

    }

}
