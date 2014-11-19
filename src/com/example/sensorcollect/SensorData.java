package com.example.sensorcollect;

public class SensorData{
	private String id;
	private float[] acceleration;
	private float[] gyroscope;
	private float[] magnetometer;
	private float[] orient;
    private String type;
	private String timeStamp;
	public SensorData(){
		this.acceleration=new float[3];
		this.gyroscope=new float[3];
		this.magnetometer=new float[3];
		this.orient=new float[3];
	}

    public SensorData clone(){
        SensorData data=new SensorData();
        data.id=this.id;
        for(int i=0;i<3;i++){
            data.acceleration[i]=this.acceleration[i];
            data.gyroscope[i]=this.gyroscope[i];
            data.magnetometer[i]=this.magnetometer[i];
            data.orient[i]=this.orient[i];
        }
        data.timeStamp=this.timeStamp;
        return data;
    }

    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("id:"+this.id+"\n");
        builder.append("accleration:"+acceleration[0]+" "+acceleration[1]+" "+acceleration[2]+"\n");
        builder.append("gyroscope:"+gyroscope[0]+" "+gyroscope[1]+" "+gyroscope[2]+"\n");
        builder.append("magnetometer:"+magnetometer[0]+" "+magnetometer[1]+" "+magnetometer[2]+"\n");
        builder.append("orient:"+orient[0]+" "+orient[1]+" "+orient[2]+"\n");
        builder.append("timestamp"+timeStamp);
        return builder.toString();
    }

    public float[] getAcceleration() {
		return acceleration;
	}
	public void setAcceleration(float[] acceleration) {
		this.acceleration = acceleration;
	}
	public float[] getGyroscope() {
		return gyroscope;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setGyroscope(float[] gyroscope) {
		this.gyroscope = gyroscope;
	}
	public float[] getMagnetometer() {
		return magnetometer;
	}
	public void setMagnetometer(float[] magnetometer) {
		this.magnetometer = magnetometer;
	}
	public float[] getOrient() {
		return orient;
	}
	public void setOrient(float[] orient) {
		this.orient = orient;
	}
	public String getTimeStamp() {
		return timeStamp;
	}
	public void setTimeStamp(String timeStamp) {
		this.timeStamp = timeStamp;
	}

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
