package com.homecontrol;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.heywatchme.backgroundwebsocket.broadcastreceiver.Restarter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MyService extends Service {


    public static final long INTERVAL=10000;//variable to execute services every 10 second
    private Handler mHandler=new Handler(); // run on another Thread to avoid crash
    private Timer mTimer=null;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Service Started", Toast.LENGTH_SHORT).show();


        if(mTimer!=null)
            mTimer.cancel();
        else
            mTimer=new Timer();
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(),0,INTERVAL);


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, Restarter.class);
        this.sendBroadcast(broadcastIntent);
    }



    public void printLogTextFile(String text)
    {
        File logFile = new File("sdcard/cordic.file");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();
            }
            catch (IOException e)
            {

                e.printStackTrace();
            }
        }
        try
        {

            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e)
        {

            e.printStackTrace();
        }
    }



    private String getTime()
    {
        Calendar calendar = Calendar.getInstance();
        int hour24hrs = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        String time=" Background Websocket App Service last run time -:" + hour24hrs + ":" + minutes +":"+ seconds;
        return  time;

    }



    private class TimeDisplayTimerTask extends TimerTask {
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.i("BackGroundService********","timerruning");
                }
            });
        }
    }



}
