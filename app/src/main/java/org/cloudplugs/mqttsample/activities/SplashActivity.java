package org.cloudplugs.mqttsample.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cloudplugs.mqttsample.MQTTService;
import org.cloudplugs.mqttsample.R;

import static org.cloudplugs.mqttsample.MQTTService.MQTT_CONNECT;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_CONNECTED;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_CONNECTION_ERROR;


public class SplashActivity extends AppCompatActivity {
    public String TAG = "SplashActivity";
    EditText input_username;
    EditText input_password;
    CheckBox input_remember;
    Button button_connect;
    TextView text_error;
    ProgressBar loading_icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        input_username = (EditText) findViewById(R.id.input_username);
        input_password = (EditText) findViewById(R.id.input_password);
        button_connect = (Button) findViewById(R.id.button_connect);
        input_remember = (CheckBox) findViewById(R.id.input_remember_me);
        text_error = (TextView) findViewById(R.id.text_error);
        loading_icon = (ProgressBar) findViewById(R.id.loading_icon);

        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

    }

    private void connect() {
        loading_icon.setVisibility(View.VISIBLE);
        button_connect.setEnabled(false);
        text_error.setVisibility(View.INVISIBLE);

        Context context = this.getApplicationContext();
        Intent i = new Intent(context, MQTTService.class);
        i.setAction(MQTT_CONNECT);

        String username = input_username.getText().toString();
        String password = input_password.getText().toString();

        i.putExtra("username", username);
        i.putExtra("password", password);

        context.startService(i);
    }

    BroadcastReceiver serviceReceiver;

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter connectedFilter = new IntentFilter();
        connectedFilter.addAction(MQTT_CONNECTED);
        connectedFilter.addAction(MQTT_CONNECTION_ERROR);

        try {
            this.serviceReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    loading_icon.setVisibility(View.GONE);
                    button_connect.setEnabled(true);

                    switch (intent.getAction()) {
                        case MQTT_CONNECTED:
                            Intent i = new Intent(context, FeedActivity.class);
                            context.startActivity(i);
                            break;
                        case MQTT_CONNECTION_ERROR:
                            text_error.setVisibility(View.VISIBLE);
                            break;
                    }
                }
            };
            this.registerReceiver(this.serviceReceiver, connectedFilter);
        } catch (Exception e) {
            Log.v(TAG, e.getMessage());
        }
    }
}
