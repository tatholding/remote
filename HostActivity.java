package com.remotecontrol.android;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.UUID;

public class HostActivity extends AppCompatActivity {
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final String PREF_ACCESS_CODE = "access_code";

    private TextView accessCodeTextView;
    private MaterialButton copyCodeButton;
    private TextView statusTextView;
    private String accessCode;
    private MediaProjection mediaProjection;
    private ScreenCaptureService screenCaptureService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        // Initialize views
        accessCodeTextView = findViewById(R.id.accessCodeTextView);
        copyCodeButton = findViewById(R.id.copyCodeButton);
        statusTextView = findViewById(R.id.statusTextView);

        // Generate or retrieve access code
        accessCode = getAccessCode();
        accessCodeTextView.setText(accessCode);

        // Set up copy button
        copyCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyAccessCode();
            }
        });

        // Request screen capture permission
        requestScreenCapture();
    }

    private String getAccessCode() {
        // Try to get existing access code from SharedPreferences
        String savedCode = getSharedPreferences("RemoteControl", MODE_PRIVATE)
                .getString(PREF_ACCESS_CODE, null);

        if (savedCode == null) {
            // Generate new access code if none exists
            savedCode = UUID.randomUUID().toString().substring(0, 8);
            getSharedPreferences("RemoteControl", MODE_PRIVATE)
                    .edit()
                    .putString(PREF_ACCESS_CODE, savedCode)
                    .apply();
        }

        return savedCode;
    }

    private void copyAccessCode() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Access Code", accessCode);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "کد کپی شد!", Toast.LENGTH_SHORT).show();
    }

    private void requestScreenCapture() {
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(
            projectionManager.createScreenCaptureIntent(),
            REQUEST_MEDIA_PROJECTION
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                // Start screen capture service
                Intent serviceIntent = new Intent(this, ScreenCaptureService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startService(serviceIntent);
            } else {
                Toast.makeText(this, "اجازه دسترسی به صفحه نمایش داده نشده است", 
                    Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenCaptureService != null) {
            stopService(new Intent(this, ScreenCaptureService.class));
        }
    }
} 