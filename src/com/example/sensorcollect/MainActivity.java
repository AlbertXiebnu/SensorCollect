package com.example.sensorcollect;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.*;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

import java.text.SimpleDateFormat;
import java.util.*;

import static android.media.AudioManager.*;

/*
 * 改进: 增加position字段，增加清除数据对话框。
 */
public class MainActivity extends Activity {
    private PowerManager.WakeLock wakeLock;
	private DoubleBuffer doubleBuffer;
	private MySensors mysensors;
	private SensorManager mSensorManager;
	private TextView myTextView;
	private Button bt1,bt2;
	private ProgressBar bar;
	private EditText editText;
    private Spinner typespinner;
    private Spinner postionspinner;
    private EditText ipEditText;
    private TextView tv_orient0;
    private TextView tv_orient1;
    private TextView tv_orient2;
	private Timer timer;
    private SoundPool soundPool;
    private int mCountdownId;
    private int mFinishId;
    private boolean canPlay;

	private final long delay=8000;
	private int defaultSampleTime=3;
    //采样频率最好是1000ms的整数倍，否则计算timer的间隔会有误差，最后导致采样个数会和预定的不一致。
	private final int sampleFrequency=25;
	private int interval;
	private int numSamples;
	private boolean isCollecting=false;
    private boolean isUploading=false;
    private int counter=0;

	private final static int COLLECTION_FINISH=1;
    private final static int DISPLAY_ORIENT=2;
    private String actionType;
    private String positionType;
	private static final String TAG = "sensorcollect";

	private Handler handler=new Handler(){
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what){
			    case COLLECTION_FINISH:
				    timer.cancel();
				    mysensors.stop();
                    isCollecting=false;
				    showSensorData();
				    doubleBuffer.doFinal();
                    setDataPrepared();
                    playMusic(mFinishId,5);
				    break;
                case DISPLAY_ORIENT:
                    Bundle data=msg.getData();
                    tv_orient0.setText(String.valueOf(data.getFloat("orient0")));
                    tv_orient1.setText(String.valueOf(data.getFloat("orient1")));
                    tv_orient2.setText(String.valueOf(data.getFloat("orient2")));
                    break;
			    default:
				    break;
			}
			super.handleMessage(msg);
		}
	};

    //将数据标识置为true，表示当前数据可以上传
    private void setDataPrepared(){
        final SharedPreferences pref=getSharedPreferences(TAG,Activity.MODE_PRIVATE);
        Boolean hasData=pref.getBoolean("hasData",false);
        if(!hasData){
            SharedPreferences.Editor editor=pref.edit();
            editor.putBoolean("hasData",true);
            editor.commit();
        }
    }

    //将数据准备好的标志置为false,表示当前没有数据可以上传
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


    private void acquireWakeLock(){
        if(wakeLock==null){
            Log.i(TAG,"acquiring wak lock");
            PowerManager pm= (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock=pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,this.getClass().getCanonicalName());
            wakeLock.acquire();
        }
    }

    private void releaseWakeLock(){
        if(null!=wakeLock&&wakeLock.isHeld()){
            wakeLock.release();
            wakeLock=null;
        }
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate() call");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		doubleBuffer=new DoubleBuffer(new DbManager(this));
		myTextView=(TextView) findViewById(R.id.textView01);
		bt1=(Button) findViewById(R.id.button1);
		bt2=(Button) findViewById(R.id.button2);
		bar=(ProgressBar) findViewById(R.id.progressBar1);
		editText=(EditText) findViewById(R.id.edit_text);
		editText.setText(String.valueOf(this.defaultSampleTime));
        editText= (EditText) findViewById(R.id.edit_text);
        tv_orient0= (TextView) findViewById(R.id.orient0);
        tv_orient1= (TextView) findViewById(R.id.orient1);
        tv_orient2= (TextView) findViewById(R.id.orient2);
		mSensorManager=(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
		mysensors=new MySensors();
		interval=Math.round(1000/sampleFrequency);
        isCollecting=false;
        isUploading=false;

        ipEditText= (EditText) findViewById(R.id.ipEditText);
        ipEditText.setText("219.224.30.83");

        //获取手机信息
        TelephonyManager tm= (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        final String imei=tm.getDeviceId();
        final String phoneNumber=tm.getLine1Number();

        //type spinner
        typespinner= (Spinner) findViewById(R.id.typespinner);
        String[] mItems=getResources().getStringArray(R.array.actionType);
        ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,mItems);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typespinner.setAdapter(arrayAdapter);
        actionType=mItems[0];
        typespinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String str=adapterView.getItemAtPosition(i).toString();
                actionType=str;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //position spinner
        postionspinner= (Spinner) findViewById(R.id.positionspinner);
        String[] postionOptions=getResources().getStringArray(R.array.positionType);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,postionOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        postionspinner.setAdapter(adapter);
        positionType=postionOptions[0];
        postionspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String str=adapterView.getItemAtPosition(i).toString();
                positionType=str;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //sound pool
        soundPool=new SoundPool(1,AudioManager.STREAM_MUSIC,0);
        mCountdownId=soundPool.load(this,R.raw.countdown,1);
        mFinishId=soundPool.load(this,R.raw.ring,1);
        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if(0==status){
                    canPlay=true;
                }else{
                    canPlay=false;
                }
            }
        });

		bt1.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                //如果没有启动采集任务，则启动，否则提示任务正在进行
                if(!isCollecting) {
                    doubleBuffer.clear();
                    myTextView.setText("");
                    final String content = editText.getText().toString();
                    int durition = Integer.parseInt(content);
                    durition = durition * 1000;
                    numSamples = durition / interval;
                    bar.setMax(durition);
                    bar.setProgress(0);
                    final UUID uuid = UUID.randomUUID();
                    counter = 0;
                    final String action=actionType;
                    final String position=positionType;
                    mysensors.start(); //打开传感器
                    timer = new Timer(); //开始计时运行
                    isCollecting = true;
                    playMusic(mCountdownId,1);
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (doubleBuffer.getCount() < numSamples) {
                                SensorData sd = mysensors.getSensorData();
                                sd.setType(action);
                                sd.setPosition(position);
                                sd.setImei(imei);
                                sd.setNumber(phoneNumber);
                                sd.setSeq(++counter);
//                                Log.i(TAG, "counter:" + counter);
                                sd.setUuid(uuid);
                                doubleBuffer.push(sd);
                                bar.incrementProgressBy(interval);
//                                Bundle bundle=new Bundle();
//                                bundle.putFloat("orient0",sd.getOrient()[0]);
//                                bundle.putFloat("orient1",sd.getOrient()[1]);
//                                bundle.putFloat("orient2",sd.getOrient()[2]);
//                                Message msg=new Message();
//                                msg.setData(bundle);
//                                msg.what=DISPLAY_ORIENT;
//                                handler.sendMessage(msg);
                            } else {
                                Message msg = new Message();
                                msg.what = COLLECTION_FINISH;
                                handler.sendMessage(msg);
                            }
                        }
                    }, delay, interval);
                }else{
                    Toast.makeText(getApplicationContext(),"collecting data,wait to complete",Toast.LENGTH_LONG).show();
                }
			}
		});
		bt2.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
                if(!isCollecting) {
                    if(isDataPrepared()) {
                        if (isConnected(getApplicationContext())) {
                            if(isUploading==false) {
                                isUploading=true;
                                new DataUploadTask(getApplicationContext()).execute();
                            }else{
                                Toast.makeText(getApplicationContext(),"data is uploading",Toast.LENGTH_LONG).show();
                            }
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

        //获取电源锁，防止休眠
        acquireWakeLock();
	}

    private void playMusic(int mSoundId,int times){
        float volume=80;
        soundPool.play(mSoundId,volume,volume,1,times,1.0f);
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
            isUploading=false;
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
        int length=doubleBuffer.getCount();
		String str="There are "+length+" data in total\n";
		myTextView.setText(str);
//        for(SensorData sensorData:sensorDataList){
//            System.out.println(sensorData);
//        }
	}

	@Override
	protected void onDestroy() {
		doubleBuffer.release();
        releaseWakeLock(); //释放电源锁
        soundPool.release();
		super.onDestroy();
        Log.i(TAG,"onDestroy call");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop Call");
    }

    @Override
	protected void onPause() {
		super.onPause();
        Log.i(TAG,"onPause call");
	}

	@Override
	protected void onResume() {
		super.onResume();
        Log.i(TAG,"onResume call");
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
        private Sensor gravitySensor; //gravity sensor
        private Sensor linearAccSensor; //linear accelaration
		private float[] rotate=new float[9];
		private float[] incline=new float[9];
		private SensorData sensorData;
        private SimpleDateFormat sdf;
		
		public MySensors(){
            sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sensorData=new SensorData();
			aSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			gSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			mSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            gravitySensor=mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            linearAccSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
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
			mSensorManager.registerListener(this, aSensor, SensorManager.SENSOR_DELAY_GAME);
			mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_GAME);
			mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,gravitySensor,SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this,linearAccSensor,SensorManager.SENSOR_DELAY_GAME);
		}
		
		public void stop(){
			mSensorManager.unregisterListener(this);
		}
		
		@Override
		public void onSensorChanged(SensorEvent event) {
            boolean flag=false;
			if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
				sensorData.setAcceleration(event.values);
			}else if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE){
				sensorData.setGyroscope(event.values);
			}else if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
				sensorData.setMagnetometer(event.values);
                flag=true;
			}else if(event.sensor.getType()==Sensor.TYPE_GRAVITY){
                sensorData.setGravity(event.values);
                flag=true;
            }else if(event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION){
                sensorData.setLinearAcc(event.values);
            }else{
				return;
			}
            if(flag==true) {
                float[] orient = new float[3];
                SensorManager.getRotationMatrix(rotate, incline, sensorData.getGravity(), sensorData.getMagnetometer());
                SensorManager.getOrientation(rotate, orient);
                sensorData.setOrient(orient);
            }
			sensorData.setTimeStamp(sdf.format(new Date()));
		}
		
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
	}

}
