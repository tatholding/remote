package com.remotecontrol.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.webrtc.EglBase;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

public class GuestActivity extends AppCompatActivity {
    private TextInputEditText hostCodeEditText;
    private MaterialButton connectButton;
    private TextView statusTextView;
    private SurfaceView remoteScreenView;
    private RemoteControlService remoteControlService;
    private PeerConnectionFactory peerConnectionFactory;
    private EglBase eglBase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guest);

        // Initialize views
        hostCodeEditText = findViewById(R.id.hostCodeEditText);
        connectButton = findViewById(R.id.connectButton);
        statusTextView = findViewById(R.id.statusTextView);
        remoteScreenView = findViewById(R.id.remoteScreenView);

        // Initialize WebRTC
        initializeWebRTC();

        // Set up connect button
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hostCode = hostCodeEditText.getText().toString().trim();
                if (hostCode.isEmpty()) {
                    Toast.makeText(GuestActivity.this, "لطفاً کد میزبان را وارد کنید", 
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                connectToHost(hostCode);
            }
        });

        // Set up touch listener for remote control
        remoteScreenView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (remoteControlService != null) {
                    remoteControlService.sendTouchEvent(event);
                }
                return true;
            }
        });
    }

    private void initializeWebRTC() {
        eglBase = EglBase.create();
        PeerConnectionFactory.InitializationOptions initializationOptions =
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions();
        PeerConnectionFactory.initialize(initializationOptions);

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .createPeerConnectionFactory();
    }

    private void connectToHost(String hostCode) {
        statusTextView.setText("در حال اتصال به میزبان...");
        connectButton.setEnabled(false);

        // Start remote control service
        Intent serviceIntent = new Intent(this, RemoteControlService.class);
        serviceIntent.putExtra("hostCode", hostCode);
        startService(serviceIntent);

        // Show remote screen view
        remoteScreenView.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remoteControlService != null) {
            stopService(new Intent(this, RemoteControlService.class));
        }
        if (eglBase != null) {
            eglBase.release();
        }
    }
} 