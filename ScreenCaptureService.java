package com.remotecontrol.android;

import android.app.Service;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

public class ScreenCaptureService extends Service {
    private MediaProjection mediaProjection;
    private VideoCapturer videoCapturer;
    private VideoSource videoSource;
    private VideoTrack videoTrack;
    private EglBase eglBase;
    private PeerConnectionFactory peerConnectionFactory;
    private SignalingClient signalingClient;

    @Override
    public void onCreate() {
        super.onCreate();
        eglBase = EglBase.create();
        initializeWebRTC();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            if (resultCode != -1 && data != null) {
                startScreenCapture(resultCode, data);
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

    private void startScreenCapture(int resultCode, Intent data) {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);

        videoCapturer = new ScreenCapturerAndroid(data, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                stopSelf();
            }
        });

        videoSource = peerConnectionFactory.createVideoSource(videoCapturer.isScreencast());
        videoCapturer.initialize(eglBase.getEglBaseContext(), this, videoSource.getCapturerObserver());

        videoTrack = peerConnectionFactory.createVideoTrack("screen_track", videoSource);

        // Start streaming
        startStreaming();
    }

    private void startStreaming() {
        // Configure video constraints
        MediaConstraints constraints = new MediaConstraints();
        constraints.mandatory.add(
            new MediaConstraints.KeyValuePair("maxWidth", "1280"));
        constraints.mandatory.add(
            new MediaConstraints.KeyValuePair("maxHeight", "720"));
        constraints.mandatory.add(
            new MediaConstraints.KeyValuePair("maxFrameRate", "30"));

        // Start the video capturer
        videoCapturer.startCapture(1280, 720, 30);

        // Connect to signaling server
        signalingClient.connect();
    }

    @Override
    public void onDestroy() {
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (videoSource != null) {
            videoSource.dispose();
        }
        if (videoTrack != null) {
            videoTrack.dispose();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
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
} 