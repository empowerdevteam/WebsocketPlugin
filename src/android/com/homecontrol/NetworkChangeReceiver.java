package com.group.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.util.Log;

class NetworkChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN);
        switch (wifiStateExtra) {
            case WifiManager.WIFI_STATE_ENABLED:
                Log.i("BroadcastReceiver11","enable");

                break;
            case WifiManager.WIFI_STATE_DISABLED:
                Log.i("BroadcastReceiver11","disable");
                break;
        }
    }
}
