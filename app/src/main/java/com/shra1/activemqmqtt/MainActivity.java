package com.shra1.activemqmqtt;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.shra1.activemqmqtt.services.BGService;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {
    public static final String serverURI = "tcp://192.168.137.1:1883";
    public static final String TAG = "ShraX";
    public static String clientId;
    private static MainActivity INSTANCE = null;
    public TextView tvRecievedMessage, tvServiceStatus;
    MqttAndroidClient mqttAndroidClient;
    Button bSubOnActivity;
    Button bSubInService;
    private Context mCtx;

    public static MainActivity getInstance() {
        return INSTANCE;
    }


    @Override
    protected void onPause() {
        super.onPause();
        INSTANCE = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        INSTANCE = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCtx = this;
        INSTANCE = this;

        bSubOnActivity = findViewById(R.id.bSubOnActivity);
        bSubOnActivity.setEnabled(false);
        bSubInService = findViewById(R.id.bSubInService);
        tvRecievedMessage = findViewById(R.id.tvRecievedMessage);
        tvServiceStatus=findViewById(R.id.tvServiceStatus);

        checkServiceStatus();

        bSubOnActivity.setOnClickListener(b -> {
            try {
                clientId = "AndroidClient";
                mqttAndroidClient = new MqttAndroidClient(mCtx, serverURI, clientId);

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
        });

        bSubInService.setOnClickListener(b -> {
            Button bx = (Button) b;
            if (checkServiceStatus()){
                bx.setText("Start service");
                Intent i = new Intent(mCtx, BGService.class);
                stopService(i);
                checkServiceStatus();
            }else{
                bx.setText("Stop service");
                Intent i = new Intent(mCtx, BGService.class);
                startService(i);
                checkServiceStatus();
            }
        });

    }

    private boolean checkServiceStatus() {
        if (isMyServiceRunning(mCtx, BGService.class)){
            String text = "Service is <font color='#00ff00'>Running</font>";
            Spanned spanned = Html.fromHtml(text);
            tvServiceStatus.setText(spanned);
            return true;
        }else{
            String text = "Service is <font color='#ff0000'>Not Running</font>";
            Spanned spanned = Html.fromHtml(text);
            tvServiceStatus.setText(spanned);
            return false;
        }
    }

    private void subscribe() {
        String subject = "English";
        try {
            mqttAndroidClient.subscribe(subject, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, final MqttMessage message) throws Exception {
                    Log.d(TAG, "messageArrived: ");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvRecievedMessage.setText(new String(message.getPayload()));
                        }
                    });

                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(2000);


                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void showMessageOnTextView(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvRecievedMessage.setText(s);
            }
        });
    }


    public static boolean isMyServiceRunning(Context c , Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
