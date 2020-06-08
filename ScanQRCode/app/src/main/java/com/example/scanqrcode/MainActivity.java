package com.example.scanqrcode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonScan, buttonConnect;
    private TextView textViewIP, textViewPort;
    private IntentIntegrator qrScan;

    private SocketHandler socketHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonScan = (Button) findViewById(R.id.buttonScan);
        textViewIP = (TextView) findViewById(R.id.textViewIP);
        textViewPort = (TextView) findViewById(R.id.textViewPort);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);

        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setPrompt("Place a QR code which contains IP & Port for socket connection.");

        buttonScan.setOnClickListener(this);
        buttonConnect.setOnClickListener(this);

        socketHandler = SocketHandler.getInstance();

        loadSocketInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_SHORT).show();
            } else {
                try {
                    JSONObject obj = new JSONObject(result.getContents());
                    textViewIP.setText(obj.getString("ip"));
                    textViewPort.setText(obj.getString("port"));
                    this.onClick(buttonConnect);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onClick(View view) {
        if ((Button) view == buttonScan) {
            qrScan.initiateScan();
        } else if ((Button) view == buttonConnect) {
//            Toast.makeText(this, "Connected", Toast.LENGTH_LONG).show();
            findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
            socketHandler.connect(this, textViewIP.getText().toString(), Integer.parseInt(textViewPort.getText().toString()));
        }
    }

    public void saveSocketInfo(){
        SharedPreferences mPrefs = getSharedPreferences("Socket", 0);
        SharedPreferences.Editor mEditor = mPrefs.edit();
        mEditor.putString("ip", textViewIP.getText().toString()).apply();
        mEditor.putString("port", textViewPort.getText().toString()).apply();
    }

    public void loadSocketInfo(){
        SharedPreferences mPrefs = getSharedPreferences("Socket", 0);
        String ip = mPrefs.getString("ip", "0.0.0.0");
        String port = mPrefs.getString("port", "8090");
        textViewIP.setText(ip);
        textViewPort.setText(port);
    }

    public void startSnapActivity() {
        saveSocketInfo();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            }
        });
        Intent intent = new Intent(this, SnapActivity.class);
        Bundle extras = new Bundle();
        startActivity(intent);
    }
}
