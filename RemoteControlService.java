package com.remotecontrol.android;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.view.MotionEvent;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

public class RemoteControlService extends Service {
    private String hostCode;
    private SignalingClient signalingClient;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase eglBase;
    private VideoTrack remoteVideoTrack;
    private SurfaceViewRenderer remoteViewRenderer;

    @Override
    public void onCreate() {
        super.onCreate();
        eglBase = EglBase.create();
        initializeWebRTC();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            hostCode = intent.getStringExtra("hostCode");
            if (hostCode != null) {
                connectToHost();
            }
        }
        return START_STICKY;
    }

    private void initializeWebRTC() {
        PeerConnectionFactory.InitializationOptions initializationOptions =
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory();

        signalingClient = new SignalingClient(this);
    }

    private void connectToHost() {
        // Connect to signaling server
        signalingClient.connect();
        
        // Send connection request with host code
        signalingClient.sendConnectionRequest(hostCode);
    }

    public void sendTouchEvent(MotionEvent event) {
        if (signalingClient != null) {
            // Convert touch event to a format that can be sent over the network
            TouchEvent touchEvent = new TouchEvent(
                event.getAction(),
                event.getX(),
                event.getY(),
                event.getPressure(),
                event.getSize()
            );
            signalingClient.sendTouchEvent(touchEvent);
        }
    }

    public void setRemoteVideoTrack(VideoTrack track) {
        this.remoteVideoTrack = track;
        if (remoteViewRenderer != null) {
            track.addSink(remoteViewRenderer);
        }
    }

    public void setRemoteViewRenderer(SurfaceViewRenderer renderer) {
        this.remoteViewRenderer = renderer;
        if (remoteVideoTrack != null) {
            remoteVideoTrack.addSink(renderer);
        }
    }

    @Override
    public void onDestroy() {
        if (remoteVideoTrack != null) {
            remoteVideoTrack.dispose();
        }
        if (remoteViewRenderer != null) {
            remoteViewRenderer.release();
        }
        if (eglBase != null) {
            eglBase.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class TouchEvent {
        private final int action;
        private final float x;
        private final float y;
        private final float pressure;
        private final float size;

        public TouchEvent(int action, float x, float y, float pressure, float size) {
            this.action = action;
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.size = size;
        }

        // Getters
        public int getAction() { return action; }
        public float getX() { return x; }
        public float getY() { return y; }
        public float getPressure() { return pressure; }
        public float getSize() { return size; }
    }
} 