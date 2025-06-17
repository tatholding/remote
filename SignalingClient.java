package com.remotecontrol.android;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import java.util.concurrent.TimeUnit;

public class SignalingClient {
    private static final String TAG = "SignalingClient";
    private static final String SIGNALING_SERVER_URL = "wss://remotepanel.liara.run/";
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final int RECONNECT_DELAY_MS = 3000;

    private final Context context;
    private final OkHttpClient client;
    private final Gson gson;
    private WebSocket webSocket;
    private int reconnectAttempts = 0;
    private boolean isConnected = false;

    public SignalingClient(Context context) {
        this.context = context;
        this.client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
    }

    public void connect() {
        Request request = new Request.Builder()
            .url(SIGNALING_SERVER_URL)
            .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                Log.d(TAG, "WebSocket connection opened");
                isConnected = true;
                reconnectAttempts = 0;
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                handleSignalingMessage(text);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                isConnected = false;
                attemptReconnect();
            }
        });
    }

    private void attemptReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            Log.d(TAG, "Attempting to reconnect... Attempt " + reconnectAttempts);
            client.newCall(new Request.Builder()
                .url(SIGNALING_SERVER_URL)
                .build())
                .enqueue(new okhttp3.Callback() {
                    @Override
                    public void onFailure(okhttp3.Call call, java.io.IOException e) {
                        Log.e(TAG, "Reconnect failed: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(okhttp3.Call call, Response response) {
                        if (response.isSuccessful()) {
                            connect();
                        }
                    }
                });
        }
    }

    public void sendConnectionRequest(String hostCode) {
        if (isConnected) {
            SignalingMessage message = new SignalingMessage(
                SignalingMessage.Type.CONNECTION_REQUEST,
                hostCode
            );
            sendMessage(message);
        }
    }

    public void sendIceCandidate(IceCandidate candidate) {
        if (isConnected) {
            SignalingMessage message = new SignalingMessage(
                SignalingMessage.Type.ICE_CANDIDATE,
                gson.toJson(candidate)
            );
            sendMessage(message);
        }
    }

    public void sendSessionDescription(SessionDescription description) {
        if (isConnected) {
            SignalingMessage message = new SignalingMessage(
                SignalingMessage.Type.SESSION_DESCRIPTION,
                gson.toJson(description)
            );
            sendMessage(message);
        }
    }

    public void sendTouchEvent(RemoteControlService.TouchEvent event) {
        if (isConnected) {
            SignalingMessage message = new SignalingMessage(
                SignalingMessage.Type.TOUCH_EVENT,
                gson.toJson(event)
            );
            sendMessage(message);
        }
    }

    private void sendMessage(SignalingMessage message) {
        if (webSocket != null) {
            webSocket.send(gson.toJson(message));
        }
    }

    private void handleSignalingMessage(String message) {
        try {
            SignalingMessage signalingMessage = gson.fromJson(message, SignalingMessage.class);
            switch (signalingMessage.getType()) {
                case CONNECTION_REQUEST:
                    handleConnectionRequest(signalingMessage.getData());
                    break;
                case ICE_CANDIDATE:
                    handleIceCandidate(signalingMessage.getData());
                    break;
                case SESSION_DESCRIPTION:
                    handleSessionDescription(signalingMessage.getData());
                    break;
                case TOUCH_EVENT:
                    handleTouchEvent(signalingMessage.getData());
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling signaling message: " + e.getMessage());
        }
    }

    private void handleConnectionRequest(String data) {
        // Handle connection request from guest
    }

    private void handleIceCandidate(String data) {
        // Handle ICE candidate from peer
    }

    private void handleSessionDescription(String data) {
        // Handle session description from peer
    }

    private void handleTouchEvent(String data) {
        // Handle touch event from guest
    }

    private static class SignalingMessage {
        public enum Type {
            CONNECTION_REQUEST,
            ICE_CANDIDATE,
            SESSION_DESCRIPTION,
            TOUCH_EVENT
        }

        private final Type type;
        private final String data;

        public SignalingMessage(Type type, String data) {
            this.type = type;
            this.data = data;
        }

        public Type getType() { return type; }
        public String getData() { return data; }
    }
} 