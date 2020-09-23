package com.homecontrol;

import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.homecontrol.SocketConfig.debug_message;
import static com.homecontrol.SocketConfig.retry_polling_interval;
import static com.homecontrol.SocketConfig.retry_timeout_min;


public class CordovaWebsocketPlugin extends CordovaPlugin {
    private static final String TAG = "CordovaWebsocketPlugin";
    public NetworkChangeReceiver networkReceiver;

    private static Map<String, WebSocketAdvanced> webSockets = new ConcurrentHashMap<String, WebSocketAdvanced>();

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        networkReceiver = new NetworkChangeReceiver();

        Log.d(TAG, "Initializing CordovaWebsocketPlugin");
    }
// Sharing Concurrent Hashmap with other Thread as it is thread safe
    public static Map<String, WebSocketAdvanced> getApplicationWebSocket() {
        return webSockets;
    }


    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("wsConnect")) {
            this.wsConnect(args, callbackContext);
        } else if (action.equals("wsAddListeners")) {
            this.wsAddListeners(args, callbackContext);
        } else if (action.equals("wsSend")) {
            this.wsSend(args, callbackContext);
        } else if (action.equals("wsClose")) {
            this.wsClose(args, callbackContext);
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        closeAllSockets();
        cordova.getActivity().unregisterReceiver(networkReceiver);
    }

    @Override
    public void onReset() {
        super.onReset();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        cordova.getActivity().registerReceiver(networkReceiver, intentFilter);
    }

    private void closeAllSockets() {
        for (WebSocketAdvanced ws : this.webSockets.values()) {
            ws.close(1000, "Disconnect");
        }
        this.webSockets.clear();
    }

    private void wsConnect(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject wsOptions = args.getJSONObject(0);
            WebSocketAdvanced ws = new WebSocketAdvanced(wsOptions, callbackContext);
            this.webSockets.put(ws.webSocketId, ws);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

    }

    private void wsAddListeners(JSONArray args, CallbackContext recvCallbackContext) {
        try {
            String webSocketId = args.getString(0);
            boolean flushRecvBuffer = args.getBoolean(1);
            WebSocketAdvanced ws = this.webSockets.get(webSocketId);
            if (ws != null) {
                ws.setRecvListener(recvCallbackContext, flushRecvBuffer);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void wsSend(JSONArray args, CallbackContext callbackContext) {
        try {
            String webSocketId = args.getString(0);
            String message = args.getString(1);

            WebSocketAdvanced ws = this.webSockets.get(webSocketId);
            ws.send(message);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void wsClose(JSONArray args, CallbackContext callbackContext) {
        try {
            String webSocketId = args.getString(0);
            int code = args.getInt(1);
            String reason = args.getString(2);

            WebSocketAdvanced ws = this.webSockets.get(webSocketId);
            ws.close(code, reason);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }


    public class WebSocketAdvanced extends WebSocketListener {

        private WebSocket webSocket;
        private CallbackContext callbackContext;
        private CallbackContext recvCallbackContext = null;
        private ArrayList<PluginResult> messageBuffer;
        private OkHttpClient client;
        private Request request;
        public String webSocketId;
        public SocketStatus socketStatus = SocketStatus.DISCONNECTED;
        private boolean isReconnectionThread = false;


        public WebSocketAdvanced(JSONObject wsOptions, final CallbackContext callbackContext) {
            try {
                this.callbackContext = callbackContext;
                this.webSocketId = UUID.randomUUID().toString();
                this.messageBuffer = new ArrayList<PluginResult>();

                String wsUrl = wsOptions.getString("url");
                int timeout = wsOptions.optInt("timeout", 0);
                int pingInterval = wsOptions.optInt("pingInterval", 0);
                JSONObject wsHeaders = wsOptions.optJSONObject("headers");
                boolean acceptAllCerts = wsOptions.optBoolean("acceptAllCerts", false);

                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                Request.Builder requestBuilder = new Request.Builder();

                clientBuilder.readTimeout(timeout, TimeUnit.MILLISECONDS);
                clientBuilder.pingInterval(pingInterval, TimeUnit.MILLISECONDS);

                if (wsUrl.startsWith("wss://") && acceptAllCerts) {
                    try {
                        final X509TrustManager gullibleTrustManager = new GullibleTrustManager();
                        final HostnameVerifier gullibleHostnameVerifier = new GullibleHostnameVerifier();
                        final SSLContext sslContext = SSLContext.getInstance("SSL");
                        KeyManager[] keyManagers = null;
                        TrustManager[] trustManagers = new TrustManager[]{gullibleTrustManager};
                        SecureRandom secureRandom = new SecureRandom();
                        sslContext.init(keyManagers, trustManagers, secureRandom);

                        // Create an ssl socket factory with our all-trusting manager
                        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                        clientBuilder.sslSocketFactory(sslSocketFactory, gullibleTrustManager);
                        clientBuilder.hostnameVerifier(gullibleHostnameVerifier);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                    }
                }

                requestBuilder.url(wsUrl);

                if (wsHeaders != null) {
                    Iterator<String> headerNames = wsHeaders.keys();
                    while (headerNames.hasNext()) {
                        String headerName = headerNames.next();
                        String headerValue = wsHeaders.getString(headerName);
                        requestBuilder.addHeader(headerName, headerValue);
                    }
                }

                this.client = clientBuilder.build();
                this.request = requestBuilder.build();

                final WebSocketAdvanced self = this;

                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        self.webSocket = client.newWebSocket(request, self);
                        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
                        //To Enable recoonection it has been kept in running state, it would be handled by the dispatcher service.
                        // self.client.dispatcher().executorService().shutdown();
                    }
                });

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        public void setRecvListener(final CallbackContext recvCallbackContext, boolean flushRecvBuffer) {
            this.recvCallbackContext = recvCallbackContext;

            if (!this.messageBuffer.isEmpty() && flushRecvBuffer) {
                Iterator<PluginResult> messageIterator = this.messageBuffer.iterator();
                while (messageIterator.hasNext()) {
                    PluginResult message = messageIterator.next();
                    recvCallbackContext.sendPluginResult(message);
                    messageIterator.remove();
                }
            }
        }

        public boolean send(String text) {
            return this.webSocket.send(text);
        }

        public boolean send(ByteString bytes) {
            return this.webSocket.send(bytes);
        }

        public boolean close(int code, String reason) {
            return this.webSocket.close(code, reason);
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            try {
                JSONObject successResult = new JSONObject();
                successResult.put("callbackMethod", "onOpen");
                successResult.put("webSocketId", this.webSocketId);
                successResult.put("code", response.code());
                socketStatus = SocketStatus.CONNECTED;
                Log.d(debug_message,"OnOpen");
               this.callbackContext.success(successResult);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            try {
                JSONObject callbackResult = new JSONObject();

                callbackResult.put("callbackMethod", "onMessage");
                callbackResult.put("webSocketId", this.webSocketId);
                callbackResult.put("message", text);

                PluginResult result = new PluginResult(Status.OK, callbackResult);
                result.setKeepCallback(true);

                if (this.recvCallbackContext != null) {
                    this.recvCallbackContext.sendPluginResult(result);
                } else {
                    this.messageBuffer.add(result);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            try {
                JSONObject callbackResult = new JSONObject();

                callbackResult.put("callbackMethod", "onMessage");
                callbackResult.put("webSocketId", this.webSocketId);
                callbackResult.put("message", bytes.toString());

                PluginResult result = new PluginResult(Status.OK, callbackResult);
                result.setKeepCallback(true);

                if (this.recvCallbackContext != null) {
                    this.recvCallbackContext.sendPluginResult(result);
                } else {
                    this.messageBuffer.add(result);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            try {
                JSONObject callbackResult = new JSONObject();

                callbackResult.put("callbackMethod", "onClose");
                callbackResult.put("webSocketId", this.webSocketId);
                callbackResult.put("code", code);
                callbackResult.put("reason", reason);
                socketStatus = SocketStatus.DISCONNECTED;
                Log.d(debug_message, "closing");
                if (this.recvCallbackContext != null) {
                    PluginResult result = new PluginResult(Status.OK, callbackResult);
                    result.setKeepCallback(true);
                    this.recvCallbackContext.sendPluginResult(result);
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            Log.d(debug_message, "onclose");
            socketStatus = SocketStatus.DISCONNECTED;

        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            Log.d(debug_message, "OnFailure");
            socketStatus = SocketStatus.DISCONNECTED;
                reconnect();

        }


        public void reconnect() {

          if (isReconnectionThread){
              LOG.d(debug_message,"Already Running Thread");
              return;
          }
            final WebSocketAdvanced self = this;
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    isReconnectionThread = true;
                    long remaining_duration = retry_timeout_min * 60;
                    if (socketStatus == SocketStatus.DISCONNECTED) {
                        while (remaining_duration > 0) {

                            try {
                                if (self.webSocket != null) {
                                    self.webSocket.close(SocketConfig.socket_close_code, "Disconnect");
                                    self.webSocket = null;
                                }
                                self.webSocket = client.newWebSocket(request, self);

                            } catch (Exception e) {
                                Log.e(debug_message, "" + e.getMessage());
                                self.webSocket = null;
                                socketStatus = SocketStatus.DISCONNECTED;

                            }
                            try {
                                Thread.sleep(retry_polling_interval);
                            } catch (InterruptedException e) {
                                self.webSocket = null;
                                e.printStackTrace();
                            }
                            remaining_duration = remaining_duration - ((retry_polling_interval) / (1000));
                            Log.d(debug_message,""+remaining_duration);
                            if (socketStatus == SocketStatus.CONNECTED){
                                LOG.d(debug_message,"Exiting Thread");
                                isReconnectionThread = false;
                                return;
                            }

                        }// End While
                        isReconnectionThread = false;
                        LOG.d(debug_message,"TimeOut Reached.Exiting Thread");
                        //Shutdown isn't necessary: Cannot be called as it will block future subsequent reconnects
                        // It will be handled by the dispatcher
                        //https://square.github.io/okhttp/3.x/okhttp/index.html?okhttp3/OkHttpClient.html
                        //self.client.dispatcher().executorService().shutdown();

                    }

                }

            });
        }

    }

    private class GullibleTrustManager implements X509TrustManager {
        private static final String TAG = "GullibleTrustManager";

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] x509Certificates = new X509Certificate[0];
            return x509Certificates;
        }

        @Override
        public void checkServerTrusted(final X509Certificate[] chain,
                                       final String authType) throws CertificateException {
            Log.d(TAG, "authType: " + String.valueOf(authType));
        }

        @Override
        public void checkClientTrusted(final X509Certificate[] chain,
                                       final String authType) throws CertificateException {
            Log.d(TAG, "authType: " + String.valueOf(authType));
        }
    }

    ;

    private class GullibleHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}