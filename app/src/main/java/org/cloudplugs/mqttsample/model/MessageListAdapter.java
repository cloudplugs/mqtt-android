package org.cloudplugs.mqttsample.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.cloudplugs.mqttsample.R;

import java.util.List;

public class MessageListAdapter extends ArrayAdapter<MessageModel> {

    public MessageListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public MessageListAdapter(Context context, int resource, List<MessageModel> items) {
        super(context, resource, items);
    }

    @Override
    public MessageModel getItem(int position) {
        return super.getItem(getCount() - position - 1);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.message_list_entry, null);
        }

        MessageModel p = getItem(position);

        if (p != null) {
            TextView text_data = (TextView) v.findViewById(R.id.text_data);
            TextView text_message = (TextView) v.findViewById(R.id.text_message);

            if (text_message != null) {
                text_message.setText(p.payload);
            }

            if (text_data != null) {
                text_data.setText(p.date.toString());
            }
        }

        return v;
    }

}
