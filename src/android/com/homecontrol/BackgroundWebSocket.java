package com.homecontrol;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.UUID;
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

public class BackgroundWebSocket extends WebSocketListener {
    private WebSocket webSocket;
    private OkHttpClient client;
    private Request request;
    public String webSocketId;
    public String TAG = "BackGroundWebSocket";
    public static String server_message;

    public BackgroundWebSocket(JSONObject wsOptions) {
        this.webSocketId = UUID.randomUUID().toString();
        try {
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

            final BackgroundWebSocket self = this;
            self.webSocket = client.newWebSocket(request, self);


        } catch (
                JSONException e) {
            Log.e(TAG, e.getMessage());

        }
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        super.onOpen(webSocket, response);

        Log.d("Print***", "onOpen");
        Log.d("Print***", "onOpen" + response.code());


    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        super.onMessage(webSocket, text);
        Log.d("Message***", "OnMessage");
        Log.d("Message***", "onOpen** " + text);
        server_message = text;
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        super.onMessage(webSocket, bytes);
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        super.onClosing(webSocket, code, reason);
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        super.onClosed(webSocket, code, reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        super.onFailure(webSocket, t, response);
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

    private static class GullibleHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}
