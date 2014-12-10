package com.example.sensorcollect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import android.util.Log;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
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
        String dirName=prepareDirectory();
        if(dirName==null) return false;

        String url="http://"+ip+":8080/sensordata/upload/data";
        ExecutorService pool= Executors.newFixedThreadPool(5);
        int count=dbManager.getRowCount();
        int start=0;
        int batchSize=DbManager.getBatch_size();
        boolean isUploadSuccess=true;
        List<Future> futures=new ArrayList<Future>();
        while(start<count){
            Callable callable=new UploadThread(start,batchSize,url,dirName);
            Future future=pool.submit(callable);
            futures.add(future);
            start=start+batchSize;
        }

        for(Future future:futures){

            try {
                if(future.get().toString().equals(WebStatus.UPLOAD_FAILED)){
                    isUploadSuccess=false;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        if(isUploadSuccess){
            return finishUpload();
        }else{
            return false;
        }
	}

    public String prepareDirectory(){
        String url="http://"+ip+":8080/sensordata/upload/prepare";
        UUID uuid=UUID.randomUUID();
        url+="?dirName="+uuid.toString();
        HttpClient client=new DefaultHttpClient();
        HttpGet request=new HttpGet(url);
        try {
            HttpResponse response=client.execute(request);
            StringBuilder builder=new StringBuilder();
            HttpEntity entity=response.getEntity();
            if(entity!=null){
                BufferedReader reader=new BufferedReader(new InputStreamReader(entity.getContent()));
                String line="";
                while((line=reader.readLine())!=null){
                    if(line.equals("")) continue;
                    builder.append(line);
                }
                reader.close();
            }
            String res=builder.toString();
            if(res.equals(WebStatus.FILE_PREPARE_SUCCESS)){
                return uuid.toString();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean finishUpload(){
        String url="http://"+ip+":8080/sensordata/upload/finish";
        HttpClient client=new DefaultHttpClient();
        HttpGet request=new HttpGet(url);
        try{
            HttpResponse response=client.execute(request);
            if(response.getStatusLine().getStatusCode()== HttpStatus.SC_OK){
                return true;
            }else{
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    class UploadThread implements Callable{
        private int start;
        private int limit;
        private String url;
        private String dirName;

        public UploadThread(int start,int limit,String url,String dirName){
            this.start=start;
            this.limit=limit;
            this.url=url;
            this.dirName=dirName;
        }

        @Override
        public Object call() throws Exception {

            HttpClient client=new DefaultHttpClient();
            HttpPost request=new HttpPost(url);
            String jsonData=dbManager.queryJsonRange(start,limit);

            List<NameValuePair> valuePairs=new ArrayList<NameValuePair>();
            valuePairs.add(new BasicNameValuePair("jsondata",jsonData));
            valuePairs.add(new BasicNameValuePair("dirName",dirName));
            request.setEntity(new UrlEncodedFormEntity(valuePairs));
//            StringEntity stringEntity=new StringEntity(jsonData);
//            stringEntity.setContentType("application/json");
//            request.setEntity(stringEntity);
            HttpResponse response=client.execute(request);
            HttpEntity entity=response.getEntity();
            StringBuilder builder=new StringBuilder();
            if(entity!=null){
                BufferedReader reader=new BufferedReader(new InputStreamReader(entity.getContent()));
                String line="";
                while((line=reader.readLine())!=null){
                    builder.append(line);
                }
                reader.close();
            }
            String status=builder.toString();
            return status;
        }
    }

}
