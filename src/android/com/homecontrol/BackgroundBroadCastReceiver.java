package com.heywatchme.backgroundwebsocket.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.heywatchme.backgroundwebsocket.backgroundservice.MyService;
import com.heywatchme.backgroundwebsocket.foregroundservice.YourService;

public class BackgroundBroadCastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("BroadcastListened", "Start ServiceS");
       // Toast.makeText(context, "Service restarted", Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          //  context.startForegroundService(new Intent(context, YourService.class));
            context.startService(new Intent(context, BackgroundService.class));
        } else {

            context.startService(new Intent(context, BackgroundService.class));
        }
    }
}
