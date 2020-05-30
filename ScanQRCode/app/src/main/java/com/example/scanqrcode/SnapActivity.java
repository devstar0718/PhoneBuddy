package com.example.scanqrcode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;

public class SnapActivity extends AppCompatActivity implements View.OnClickListener {
    private Button buttonCamera, buttonSend;
    private ImageView imageView;
    private Bitmap currentImage = null;
    private SocketHandler socketHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snap);

        buttonCamera = (Button)findViewById(R.id.buttonCamera);
        buttonSend = (Button)findViewById(R.id.buttonSend);
        imageView = (ImageView)findViewById(R.id.imageView);

        buttonSend.setOnClickListener(this);
        buttonCamera.setOnClickListener(this);

        socketHandler = SocketHandler.getInstance();
    }

    @Override
    public void onClick(View view) {
        if((Button)view == buttonCamera){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, 0);
        }
        else if((Button)view == buttonSend){
            if(currentImage == null) {
                Toast.makeText(this, "Please take a picture with Camera", Toast.LENGTH_LONG).show();
                return;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream .toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            socketHandler.send(encoded);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data == null){
            Toast.makeText(this, "No Picture captured.", Toast.LENGTH_LONG).show();
            return;
        }
        currentImage = (Bitmap)data.getExtras().get("data");
        if(currentImage != null)
            imageView.setImageBitmap(currentImage);

    }

}
