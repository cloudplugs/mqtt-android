package org.cloudplugs.mqttsample.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.cloudplugs.mqttsample.R;

import java.sql.SQLException;

public class OrmliteHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "todo";
    private static final int DATABASE_VERSION = 3;

    /**
     * The data access object used to interact with the Sqlite database to do C.R.U.D operations.
     */
    private Dao<MessageModel, Long> todoDao;

    public OrmliteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION,
                /**
                 * R.raw.ormlite_config is a reference to the ormlite_config.txt file in the
                 * /res/raw/ directory of this project
                 * */
                R.raw.ormlite_config);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {

            /**
             * creates the Todo database table
             */
            TableUtils.createTable(connectionSource, MessageModel.class);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource,
                          int oldVersion, int newVersion) {
        try {
            /**
             * Recreates the database when onUpgrade is called by the framework
             */
            TableUtils.dropTable(connectionSource, MessageModel.class, false);
            onCreate(database, connectionSource);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns an instance of the data access object
     * @return
     * @throws SQLException
     */
    public Dao<MessageModel, Long> getDao() throws SQLException {
        if(todoDao == null) {
            todoDao = getDao(MessageModel.class);
        }
        return todoDao;
    }
}

