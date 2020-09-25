package com.homecontrol;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.homecontrol.CordovaWebsocketPlugin.getApplicationWebSocket;


public class BackgroundService extends Service {


    public static final long INTERVAL=10000;//variable to execute services every 10 second
    private Handler mHandler=new Handler(); // run on another Thread to avoid crash
    private Timer mTimer=null;
    Map<String, CordovaWebsocketPlugin.WebSocketAdvanced> webSocketAdvancedMap;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        webSocketAdvancedMap = getApplicationWebSocket();

        if(mTimer!=null)
            mTimer.cancel();
        else
            mTimer=new Timer();
        mTimer.scheduleAtFixedRate(new TimeDisplayTimerTask(),0,INTERVAL);


        return START_STICKY;
    }

    private class TimeDisplayTimerTask extends TimerTask {
        @Override
        public void run() {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Servie already started", Toast.LENGTH_SHORT).show();

                    for (CordovaWebsocketPlugin.WebSocketAdvanced ws : webSocketAdvancedMap.values()) {
                        try {
                                //ws.reconnect();TODO: Create new instance of web-socket and establish new connection

                        } catch (Exception e) {
                            Log.e("Exception", e.getMessage());
                        }
                    }
                }
            });
        }
    }



}
