package com.example.sensorcollect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;


public class MainActivity extends Activity {
	private DbManager dbManager;
	private MySensors mysensors;
	private SensorManager mSensorManager;
	private TextView myTextView;
	private Button bt1,bt2;
	private ProgressBar bar;
	private EditText editText;
    private TextView textView;
    private Spinner spinner;
    private EditText ipEditText;
	private Timer timer;
	private long delay=0; //������
	private int defaultSampleTime=3; //���
	private int sampleFrequency=50;
	private int interval;
	private int numSamples;
	private boolean isCollecting=false; //���������������������������������
	private List<SensorData> sensorDataList;
	private final static int COLLECTION_FINISH=1;
	private static final String TAG = "sensorcollect";

	private Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			case COLLECTION_FINISH:
				timer.cancel();
				mysensors.stop();
				showSensorData();
				dbManager.add(sensorDataList);
                setDataPrepared();
				break;
			default:
				break;
			}
			super.handleMessage(msg);
		}
	};

    private void setDataPrepared(){
        final SharedPreferences pref=getSharedPreferences(TAG,Activity.MODE_PRIVATE);
        Boolean hasData=pref.getBoolean("hasData",false);
        if(!hasData){
            SharedPreferences.Editor editor=pref.edit();
            editor.putBoolean("hasData",true);
            editor.commit();
        }
    }

    private void resetDataPrepared(){
        final SharedPreferences pref=getSharedPreferences(TAG,Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor=pref.edit();
        editor.putBoolean("hasData",false);
        editor.commit();
    }

    private boolean isDataPrepared(){
        final SharedPreferences pref=getSharedPreferences(TAG,Activity.MODE_PRIVATE);
        Boolean hasData=pref.getBoolean("hasData",false);
        return hasData;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		dbManager=new DbManager(this);
		myTextView=(TextView) findViewById(R.id.textView01);
		bt1=(Button) findViewById(R.id.button1);
		bt2=(Button) findViewById(R.id.button2);
		bar=(ProgressBar) findViewById(R.id.progressBar1);
		editText=(EditText) findViewById(R.id.edit_text);
		editText.setText(String.valueOf(this.defaultSampleTime));
        editText= (EditText) findViewById(R.id.edit_text);
		mSensorManager=(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mysensors=new MySensors();
		sensorDataList=new ArrayList<SensorData>();
		interval=Math.round(1000/sampleFrequency);
        isCollecting=false;

        ipEditText= (EditText) findViewById(R.id.ipEditText);
        ipEditText.setText("219.224.30.83");

        //获取手机信息
        TelephonyManager tm= (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        final String imei=tm.getDeviceId();
        final String phoneNumber=tm.getLine1Number();

        textView= (TextView) findViewById(R.id.textView02);
        spinner= (Spinner) findViewById(R.id.spinner);
        String[] mItems=getResources().getStringArray(R.array.actionType);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mItems);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);
        textView.setText(mItems[0]);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String str=adapterView.getItemAtPosition(i).toString();
                textView.setText(str);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

		bt1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				sensorDataList.clear();
                myTextView.setText("");
                final String actionType=textView.getText().toString();
                String content=editText.getText().toString();
				int durition=Integer.parseInt(content);
				durition=durition*1000;
				numSamples=durition/interval;
				bar.setMax(durition);
				bar.setProgress(0);

                mysensors.start(); //打开传感器
				timer=new Timer(); //开始计时运行
                isCollecting=true;
				timer.schedule(new TimerTask(){
					@Override
					public void run() {
						if(sensorDataList.size()<numSamples){
							SensorData sd=mysensors.getSensorData();
                            sd.setType(actionType);
                            sd.setImei(imei);
                            sd.setNumber(phoneNumber);
							sensorDataList.add(sd);
							bar.incrementProgressBy(interval);
						}else{
							isCollecting=false;
							Message msg=new Message();
							msg.what=COLLECTION_FINISH;
							handler.sendMessage(msg);
						}
					}
				}, delay, interval);
			}
		});
		bt2.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                if(!isCollecting) {
                    if(isDataPrepared()) {
                        if (isConnected(getApplicationContext())) {
                            new DataUploadTask(getApplicationContext()).execute();
                        } else {
                            Toast.makeText(getApplicationContext(), "web is not connected", Toast.LENGTH_LONG).show();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),"data is not prepared",Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(),"collecting data,can not upload",Toast.LENGTH_LONG).show();
                }
			}
		});


	}
	
	private class DataUploadTask extends AsyncTask<Void,Void,String>{
        private Context context;

        public DataUploadTask(Context context){
            this.context=context;
        }

        @Override
        protected String doInBackground(Void... params) {
            String ip=ipEditText.getText().toString();
            boolean isOk=checkIp(ip);
            if(isOk){
                WebService webService=new WebService(context,ip);
                boolean res=webService.upload();
                if(res){
                    return WebStatus.UPLOAD_SUCCESSFULL;
                }else{
                    return WebStatus.UPLOAD_FAILED;
                }
            }else {
                return WebStatus.IP_Illegal;
            }

        }

        @Override
        protected void onPostExecute(String s) {
            if(s.equals(WebStatus.UPLOAD_SUCCESSFULL)){
                resetDataPrepared();
                Toast.makeText(context,"data upload successfully",Toast.LENGTH_LONG).show();
            }else if(s.equals(WebStatus.UPLOAD_FAILED)){
                Toast.makeText(context,"data upload failed",Toast.LENGTH_LONG).show();
            }else if(s.equals(WebStatus.IP_Illegal)){
                Toast.makeText(context,"ip config is not correct",Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean checkIp(String ip){
        String splits[]=ip.split("\\.");
        if(splits.length!=4) return false;
        for(int i=0;i<splits.length;i++){
            String str=splits[i];
            if(isNumberic(str)){
                int n=Integer.valueOf(str);
                if(n<0||n>255) return false;
            }else{
                return false;
            }
        }
        return true;
    }

    private boolean isNumberic(String num){
        for(int i=0;i<num.length();i++){
            int ch=num.charAt(i);
            if(ch<48||ch>57)
                return false;
        }
        return true;
    }
	
	public void showSensorData(){
        int length=sensorDataList.size();
		String str="There are "+length+" data in total\n";
		myTextView.setText(str);
//        for(SensorData sensorData:sensorDataList){
//            System.out.println(sensorData);
//        }
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		dbManager.closeDB();
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		mysensors.stop();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mysensors.start();
	}
	
	private boolean isConnected(Context context){
		ConnectivityManager cm=(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info=cm.getActiveNetworkInfo();
		if(info!=null){
			return info.isConnected();
		}else{
			return false;
		}
	}
	

	class MySensors implements SensorEventListener{
		private Sensor aSensor; //acceleration
		private Sensor gSensor; //gyroscopeSensor
		private Sensor mSensor; //magnetometer Sensor
		private float[] rotate=new float[9];
		private float[] incline=new float[9];
		private SensorData sensorData=new SensorData();
		
		public MySensors(){
			// TODO Auto-generated constructor stub
			aSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			gSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			mSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		}
		
//		public String getStatus(){
//			String str="";
//			str+="Acceleration: X("+sensorData.acceleration[0]+") Y("+sensorData.acceleration[1]+") Z("+sensorData.acceleration[2]+")\n";
//			str+="Gyroscope: X("+sensorData.gyroscope[0]+") Y("+sensorData.gyroscope[1]+") Z("+sensorData.gyroscope[2]+")\n";
//			str+="Magnetometer: X("+sensorData.magnetometer[0]+") Y("+sensorData.magnetometer[1]+") Z("+sensorData.magnetometer[2]+")\n";
//			str+="Orientation: azimuth("+sensorData.orient[0]+") pitch("+sensorData.orient[1]+") roll("+sensorData.orient[2]+")\n";
//			return str;
//		}
		
		public SensorData getSensorData(){
			return this.sensorData.clone();
		}
		
		public void start(){
			mSensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_NORMAL);
			mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_NORMAL);
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		
		public void stop(){
			mSensorManager.unregisterListener(this);
		}
		
		@Override
		public void onSensorChanged(SensorEvent event) {
			// TODO Auto-generated method stub
			if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
				sensorData.setAcceleration(event.values);
			}else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
				sensorData.setGyroscope(event.values);
			}else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
				sensorData.setMagnetometer(event.values);
			}else{
				return;
			}
			float[] orient=new float[3];
			SensorManager.getRotationMatrix(rotate, incline, sensorData.getAcceleration(), sensorData.getMagnetometer());
			SensorManager.getOrientation(rotate, orient);
			sensorData.setOrient(orient);
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sensorData.setTimeStamp(sdf.format(new Date()));
		}
		
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
