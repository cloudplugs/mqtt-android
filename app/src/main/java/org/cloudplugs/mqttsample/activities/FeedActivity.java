package org.cloudplugs.mqttsample.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.table.TableUtils;

import org.cloudplugs.mqttsample.MQTTService;
import org.cloudplugs.mqttsample.model.MessageListAdapter;
import org.cloudplugs.mqttsample.R;
import org.cloudplugs.mqttsample.model.MessageModel;
import org.cloudplugs.mqttsample.model.OrmliteHelper;

import java.sql.SQLException;
import java.util.List;

import static org.cloudplugs.mqttsample.MQTTService.MQTT_CONNECT;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_DISCONNECT;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_DISCONNECTED;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_MESSAGE_ARRIVED;
import static org.cloudplugs.mqttsample.MQTTService.MQTT_PUBLISH;

public class FeedActivity extends Activity {
    private static final String TAG = "FeedActivity";
    Button button_send;
    Button button_clear;
    Button button_logout;
    ListView list_messages;
    EditText text_message;

    MessageListAdapter messageListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        /* binding objects */
        button_clear = (Button) findViewById(R.id.button_clear);
        button_logout = (Button) findViewById(R.id.button_logout);
        list_messages = (ListView) findViewById(R.id.list_messages);
        text_message = (EditText) findViewById(R.id.text_message);
        button_send = (Button) findViewById(R.id.button_send);

        OrmliteHelper ormliteHelper = OpenHelperManager.getHelper(this, OrmliteHelper.class);
        try {
            Dao<MessageModel, Long> messagesDao = ormliteHelper.getDao();

            // query for all of the data objects in the database
            List<MessageModel> list = messagesDao.queryForAll();

            Log.v(TAG, "found "+new Integer (list.size()).toString()+" messages");

            messageListAdapter = new MessageListAdapter(this, R.layout.message_list_entry, list);
            list_messages.setAdapter(messageListAdapter);
        } catch (SQLException e) {
            Log.v(TAG, e.getMessage());
        }

        button_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearMessages();
            }
        });

        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), MQTTService.class);
                String toSend = text_message.getText().toString();
                i.setAction(MQTT_PUBLISH);
                i.putExtra("publish", toSend);
                getApplicationContext().startService(i);

                text_message.setText("");

                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        });

        button_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), MQTTService.class);
                i.setAction(MQTT_DISCONNECT);
                getApplicationContext().startService(i);
                finish();
            }
        });
    }

    private void clearMessages() {
        OrmliteHelper ormliteHelper = OpenHelperManager.getHelper(this, OrmliteHelper.class);
        try {
            TableUtils.clearTable(ormliteHelper.getConnectionSource(), MessageModel.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        populateAdapter();
    }

    private void populateAdapter() {
        OrmliteHelper ormliteHelper = OpenHelperManager.getHelper(this, OrmliteHelper.class);
        try {
            Dao<MessageModel, Long> messagesDao = ormliteHelper.getDao();

            // query for all of the data objects in the database
            List<MessageModel> list = messagesDao.queryForAll();

            Log.v(TAG, "found "+new Integer (list.size()).toString()+" messages");

            messageListAdapter.clear();
            messageListAdapter.addAll(list);
            messageListAdapter.notifyDataSetChanged();

        } catch (SQLException e) {
            Log.v(TAG, e.getMessage());
        }
    }

    BroadcastReceiver serviceReceiver;

    @Override
    protected void onResume() {
        super.onResume();

        final IntentFilter connectedFilter = new IntentFilter();
        connectedFilter.addAction(MQTT_DISCONNECTED);
        connectedFilter.addAction(MQTT_MESSAGE_ARRIVED);

        try {
            this.serviceReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    switch (intent.getAction()) {
                        case MQTT_CONNECT:
                            CharSequence text = "Connected!";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                            break;
                        case MQTT_DISCONNECTED:
                            CharSequence text_disconnected = "Disconnected!";
                            int duration_disconnected = Toast.LENGTH_SHORT;

                            Toast toast_disconnected = Toast.makeText(context, text_disconnected, duration_disconnected);
                            toast_disconnected.show();
                            finish();
                            break;
                        case MQTT_MESSAGE_ARRIVED:
                            CharSequence text_arrived = "new message";
                            int duration_arrived = Toast.LENGTH_SHORT;

                            Toast toast_arrived = Toast.makeText(context, text_arrived, duration_arrived);
                            toast_arrived.show();
                            populateAdapter();
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
