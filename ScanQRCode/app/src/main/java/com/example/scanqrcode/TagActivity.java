package com.example.scanqrcode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class TagActivity extends SocketActivity implements View.OnClickListener {
    private Button buttonPicture, buttonOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tag);
        buttonPicture = (Button) findViewById(R.id.buttonPicture);
        buttonOrder = (Button) findViewById(R.id.buttonOrder);

        buttonPicture.setOnClickListener(this);
        buttonOrder.setOnClickListener(this);

        socketHandler = SocketHandler.getInstance();
        socketHandler.currentActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.CALL_PHONE}, 111);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 111) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Log.d("Alexei", "Read Phone State is not allowed.\nYou cannot get incoming phone numbers.");
                Toast.makeText(this, "Read Phone State is not allowed.\nYou cannot get incoming phone numbers.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        if ((Button) view == buttonPicture) {
            Intent intent = new Intent(this, SnapActivity.class);
            startActivity(intent);
        } else if ((Button) view == buttonOrder) {
            Intent intent = new Intent(this, OrderActivity.class);
            startActivity(intent);
        }
    }
}
