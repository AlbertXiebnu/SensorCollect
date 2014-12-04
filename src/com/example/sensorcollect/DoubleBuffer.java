package com.example.sensorcollect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xie on 14-12-4.
 * @author xie
 * 双缓冲栈实现，解决传感器数据过多导致内存增长太快。
 * 两个缓冲栈
 * 1. current ：把新来的数据压人current
 * 2. back：执行持久化操作，完成后清空
 */
public class DoubleBuffer {
    private DbManager dbManager;
    private List<SensorData> current;
    private List<SensorData> back;
    private int maxSize=2000;
    private int count=0;

    public DoubleBuffer(DbManager dbManager){
        this.dbManager=dbManager;
        current=new ArrayList<SensorData>();
        back=new ArrayList<SensorData>();
    }

    public DoubleBuffer(DbManager dbManager,int maxSize) {
        this(dbManager);
        this.maxSize = maxSize;
    }

    private void swapBuffer(){
        back.clear();//切换之前先清空back
        List<SensorData> temp=current;
        current=back;
        back=temp;
    }

    public void push(SensorData sensorData){
        if(current.size()<maxSize){
            current.add(sensorData);
            count++;
        }else{
            swapBuffer();
            current.add(sensorData);
            count++;
            new PersistThread(back).run();
        }

    }


    public void clear(){
        this.current.clear();
        this.back.clear();
        this.count=0;
    }

    public void release(){
        dbManager.closeDB();
    }

    public void doFinal(){
        dbManager.add(current);
        current.clear();
    }


    class PersistThread implements Runnable{
        private List<SensorData> saveList;
        public PersistThread(List<SensorData> saveList){
            this.saveList=saveList;
        }

        @Override
        public void run() {
            dbManager.add(saveList);
        }
    }

    public int getCount(){
        return count;
    }
}
