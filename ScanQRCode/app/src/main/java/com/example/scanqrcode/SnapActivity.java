package com.example.scanqrcode;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SnapActivity extends SocketActivity implements View.OnClickListener {
    private Button buttonCamera, buttonSend, buttonSendHigh;
    private ImageView imageView;
    private Bitmap currentImage = null;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snap);

        buttonCamera = (Button) findViewById(R.id.buttonCamera);
        buttonSend = (Button) findViewById(R.id.buttonSend);
        imageView = (ImageView) findViewById(R.id.imageViewPicture);
        buttonSendHigh = (Button) findViewById(R.id.buttonSend2);

        buttonSend.setOnClickListener(this);
        buttonCamera.setOnClickListener(this);
        buttonSendHigh.setOnClickListener(this);

        socketHandler = SocketHandler.getInstance();
        socketHandler.currentActivity = this;
        this.onClick(buttonCamera);
    }

    @Override
    public void onClick(View view) {
        if ((Button) view == buttonCamera) {
            dispatchTakePictureIntent();
        } else if ((Button) view == buttonSend) {
            if (currentImage == null) {
                Toast.makeText(this, "Please take a picture with Camera", Toast.LENGTH_LONG).show();
                return;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            if(!socketHandler.isConnected()){
                Beep();
                Toast.makeText(this, "Socket not connected.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return;
            }
            socketHandler.sendPicture(encoded, this);
        } else if ((Button) view == buttonSendHigh) {
            if (currentImage == null) {
                Toast.makeText(this, "Please take a picture with Camera", Toast.LENGTH_LONG).show();
                return;
            }
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            currentImage.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
            if(!socketHandler.isConnected()){
                Beep();
                Toast.makeText(this, "Socket not connected.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return;
            }
            socketHandler.sendPicture(encoded, this);
        }
    }

    public void setInitialProgressRange(final int max) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setMax(max);
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonCamera).setEnabled(false);
                findViewById(R.id.buttonSend).setEnabled(false);
                findViewById(R.id.buttonSend2).setEnabled(false);
            }
        });
    }

    public void setProgressValue(final int value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (value == -1) {
                    findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                    findViewById(R.id.buttonCamera).setEnabled(true);
                    findViewById(R.id.buttonSend).setEnabled(true);
                    findViewById(R.id.buttonSend2).setEnabled(true);
//                    Beep();
                } else {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setProgress(value);
                }
            }
        });
    }

    public void Beep() {
//        ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 1000);
//        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,300);
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        r.play();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        setPic();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.scanqrcode",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private void setPic() {
        currentImage = BitmapFactory.decodeFile(currentPhotoPath, null);
        imageView.setImageBitmap(currentImage);
    }
}
