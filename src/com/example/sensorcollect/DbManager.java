package com.example.sensorcollect;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DbManager {
	private SQLiteDatabase db;
	private DbHelper helper;
    private static final String table="sensordata";
    private static final int batch_size=2000;

	public DbManager(Context context){
		helper=new DbHelper(context);
		db=helper.getWritableDatabase();
	}
    public static int getBatch_size(){
        return batch_size;
    }

	public void add(List<SensorData> datalist){
		db.beginTransaction();
		try{
			for(SensorData s:datalist){
				db.execSQL("INSERT INTO "+table+" VALUES(null,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
						new Object[]{s.getAcceleration()[0],s.getAcceleration()[1],s.getAcceleration()[2],
						s.getGyroscope()[0],s.getGyroscope()[1],s.getGyroscope()[2],s.getMagnetometer()[0],
						s.getMagnetometer()[1],s.getMagnetometer()[2],s.getOrient()[0],s.getOrient()[1],
						s.getOrient()[2],s.getType(),s.getPosition(),s.getTimeStamp(),s.getImei(),s.getNumber()});
			}
			db.setTransactionSuccessful();
		}finally{
			db.endTransaction();
		}
	}

    public void clear(){
        db.delete(table,null,null);
    }

//    private List<String> getAllRecordIds(){
//        List<String> list=new ArrayList<String>();
//        Cursor cursor=db.rawQuery("SELECT * FROM "+table,null);
//    }
	
	public String queryAllInJson(){
		List<SensorBean> datalist=queryAllMiniBatch();
        return JSON.toJSONString(datalist);
	}

    private List<SensorBean> queryAllMiniBatch(){
        int count=getRowCount();
        int offset=0;
        List<SensorBean> res=new ArrayList<SensorBean>();
        while(offset<count){
            List<SensorBean> temp=queryRange(offset,batch_size);
            res.addAll(temp);
            offset=offset+batch_size;
        }
        return res;
    }

    public String queryJsonRange(int offset,int limit){
        List<SensorBean> datalist=queryRange(offset,limit);
        return JSON.toJSONString(datalist);
    }
	
	private List<SensorBean> queryRange(int offset,int limit){
		ArrayList<SensorBean> list=new ArrayList<SensorBean>();
		Cursor cursor=db.rawQuery("SELECT * FROM "+table +" limit "+limit+" offset "+offset, null);
		while(cursor.moveToNext()){
			SensorBean sd=new SensorBean();
			//sd.setId(cursor.getInt(cursor.getColumnIndex("_id")));
			sd.setAccX(cursor.getDouble(cursor.getColumnIndex("acc_x")));
            sd.setAccY(cursor.getDouble(cursor.getColumnIndex("acc_y")));
            sd.setAccZ(cursor.getDouble(cursor.getColumnIndex("acc_z")));

			sd.setGyroX(cursor.getDouble(cursor.getColumnIndex("gyro_x")));
            sd.setGyroY(cursor.getDouble(cursor.getColumnIndex("gyro_y")));
            sd.setGyroZ(cursor.getDouble(cursor.getColumnIndex("gyro_z")));

			sd.setMagnetX(cursor.getDouble(cursor.getColumnIndex("magnet_x")));
            sd.setMagnetY(cursor.getDouble(cursor.getColumnIndex("magnet_y")));
            sd.setMagnetZ(cursor.getDouble(cursor.getColumnIndex("magnet_z")));

			sd.setOrientX(cursor.getDouble(cursor.getColumnIndex("orient_x")));
            sd.setOrientY(cursor.getDouble(cursor.getColumnIndex("orient_y")));
            sd.setOrientZ(cursor.getDouble(cursor.getColumnIndex("orient_z")));

            sd.setType(cursor.getString(cursor.getColumnIndex("type")));
            sd.setPosition(cursor.getString(cursor.getColumnIndex("position")));
			sd.setTimestamp(cursor.getString(cursor.getColumnIndex("timestamp")));
            sd.setImei(cursor.getString(cursor.getColumnIndex("imei")));
            sd.setNumber(cursor.getString(cursor.getColumnIndex("number")));
			list.add(sd);
		}
		return list;
	}

    public int getRowCount(){
        String sql="select count(*) from "+table;
        Cursor cursor=db.rawQuery(sql,null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }
	
	public void closeDB(){
		db.close();
	}
	
	
	protected void finalize(){
		closeDB();
	}
}
