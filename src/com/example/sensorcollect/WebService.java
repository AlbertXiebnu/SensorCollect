package com.example.sensorcollect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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
        String url="http://"+ip+":8080/sensordata/upload/data";
        ExecutorService pool= Executors.newFixedThreadPool(5);
        int count=dbManager.getRowCount();
        int start=0;
        int batchSize=DbManager.getBatch_size();
        boolean isSuccess=true;
        List<Future> futures=new ArrayList<Future>();
        while(start<count){
            Callable callable=new UploadThread(start,batchSize,url);
            Future future=pool.submit(callable);
            futures.add(future);
            start=start+batchSize;
        }

        for(Future future:futures){
            try {
                if(future.get().toString().equals(WebStatus.UPLOAD_FAILED)){
                    isSuccess=false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if(isSuccess) {
            dbManager.clear();
        }
        return isSuccess;
	}

    class UploadThread implements Callable{
        private int start;
        private int limit;
        private String url;

        public UploadThread(int start,int limit,String url){
            this.start=start;
            this.limit=limit;
            this.url=url;
        }

        @Override
        public Object call() throws Exception {

            HttpClient client=new DefaultHttpClient();
            HttpPost request=new HttpPost(url);
            String jsonData=dbManager.queryJsonRange(start,limit);
            StringEntity stringEntity=new StringEntity(jsonData);
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
            return status;
        }
    }

}
