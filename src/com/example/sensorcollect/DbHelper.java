package com.example.sensorcollect;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
	private static final String DATABASE_NAME="sensorcollect.db";
	private static final int VERSION=1;

	public DbHelper(Context context) {
		super(context, DATABASE_NAME, null, VERSION);
		// TODO Auto-generated constructor stub
	}
	@Override
	public void onCreate(SQLiteDatabase db) {
		// TODO Auto-generated method stub
		db.execSQL("CREATE TABLE IF NOT EXISTS sensordata "+
				"(_id INTEGER PRIMARY KEY AUTOINCREMENT,acc_x DOUBLE,acc_y DOUBLE,acc_z DOUBLE," +
				"gyro_x DOUBLE,gyro_y DOUBLE,gyro_z DOUBLE,magnet_x DOUBLE,magnet_y DOUBLE,magnet_z DOUBLE," +
				"orient_x DOUBLE,orient_y DOUBLE,orient_z DOUBLE,type VARCHAR,timestamp VARCHAR,imei VARCHAR," +
                "number VARCHAR)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
	}
}
