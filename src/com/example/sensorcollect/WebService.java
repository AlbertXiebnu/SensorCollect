package com.example.sensorcollect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;

import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

public class WebService {
	private static DbManager dbManager;
	private static Context context;
    private static final String TAG="sensorCollect";
    private String ip="";
	public WebService(Context context,String ip){
		dbManager=new DbManager(context);
        this.ip=ip;
		this.context=context;
	}
	public boolean upload(){
		String json_str=dbManager.queryAllInJson();
		StringBuffer sb=new StringBuffer();
		try {
            String url="http://"+ip+":8080/sensordata/upload/data";
			HttpClient client=new DefaultHttpClient();
			HttpPost request=new HttpPost(url);
            System.out.println(json_str);
            StringEntity stringEntity=new StringEntity(json_str);
            stringEntity.setContentType("application/json");
            request.setEntity(stringEntity);

			HttpResponse response=client.execute(request);
			HttpEntity entity=response.getEntity();
            StringBuilder builder=new StringBuilder();
			if(entity!=null){
				BufferedReader reader=new BufferedReader(new InputStreamReader(entity.getContent()));
				String line="";
				while((line=reader.readLine())!=null){
                    builder.append(line+"\n");
				}
				reader.close();
			}
			JSONObject object=new JSONObject(builder.toString());
			String status=object.getString("status");
            if(status.equals(WebStatus.UPLOAD_SUCCESSFULL)) {
                dbManager.clear();
                return true;
            }else if(status.equals(WebStatus.UPLOAD_FAILED)){
                Log.i(TAG,object.getString("errorMessage"));
                return false;
            }
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
	}

}
