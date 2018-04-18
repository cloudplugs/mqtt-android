package org.cloudplugs.mqttsample.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "messages")
public class MessageModel {
    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField
    public Date date;

    @DatabaseField
    public String payload;

    public MessageModel(){

    }

    public MessageModel(String payload, Date date) {
        this.payload = payload;
        this.date = date;
    }
}
