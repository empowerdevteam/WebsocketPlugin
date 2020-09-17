package com.homecontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.Map;

import static com.homecontrol.CordovaWebsocketPlugin.getApplicationWebSocket;

class NetworkChangeReceiver extends BroadcastReceiver {
    Map<String, CordovaWebsocketPlugin.WebSocketAdvanced> webSocketAdvancedMap;

    @Override
    public void onReceive(Context context, Intent intent) {
        int wifiStateExtra = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                WifiManager.WIFI_STATE_UNKNOWN);
        webSocketAdvancedMap = getApplicationWebSocket();

        switch (wifiStateExtra) {
            case WifiManager.WIFI_STATE_ENABLED:// TODO: Enable and Test for Mobile data connection
                for (CordovaWebsocketPlugin.WebSocketAdvanced ws : webSocketAdvancedMap.values()) {
                    try {
                        if (ws.socketStatus == SocketStatus.DISCONNECTED) {
                            ws.reconnect();
                        }
                    } catch (Exception e) {
                        Log.e("Exception", e.getMessage());
                    }
                }

                break;
            case WifiManager.WIFI_STATE_DISABLED:
                break;
        }
    }
}
