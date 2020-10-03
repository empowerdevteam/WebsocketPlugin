package com.homecontrol;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import org.apache.cordova.LOG;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.homecontrol.CordovaWebsocketPlugin.getApplicationWebSocket;


public class BackgroundService extends Service {


    public static final long INTERVAL=10000;//variable to execute services every 10 second
    private Handler mHandler=new Handler(); // run on another Thread to avoid crash
    private Timer mTimer=null;
    Map<String, CordovaWebsocketPlugin.WebSocketAdvanced> webSocketAdvancedMap;
    public static boolean serviceRunning = false;
    public  boolean backgroundService = true;
    JSONObject wsOptions;
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        webSocketAdvancedMap = getApplicationWebSocket();

        wsOptions  = new JSONObject();
        try {
            wsOptions.put("url","wss://2aqh15tm0b.execute-api.us-east-2.amazonaws.com/dev");
            wsOptions.put("timeout","5000");
            wsOptions.put("pingInterval","60000");
            wsOptions.put("acceptAllCerts","false");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(mTimer!=null)
            mTimer.cancel();
        else
            mTimer=new Timer();
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(),0,INTERVAL);


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("RunningThread****","Stop");
        mTimer.cancel();
        serviceRunning = false;
        mHandler.removeCallbacks(runnable);
        stopSelfResult(1);

    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), "Cordic Keep Alive Testing", Toast.LENGTH_SHORT).show();
            if (!serviceRunning) {
                CordovaWebsocketPlugin.WebSocketAdvanced ws = new CordovaWebsocketPlugin.WebSocketAdvanced(wsOptions, null, true);
                 Log.d("WSCode****", "" + ws);
                //Log.d("ResponseCode****",""+ws.responseCode);

            }
        }


    };


    private class TimeDisplayTimerTask extends TimerTask {
        @Override
        public void run() {
            mHandler.post(runnable);
        }
    }



}
