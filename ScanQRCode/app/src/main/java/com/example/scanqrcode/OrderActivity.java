package com.example.scanqrcode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;
import org.json.JSONObject;

public class OrderActivity extends AppCompatActivity implements View.OnClickListener {
    private IntentIntegrator qrScan;
    private TextView textViewPartNumber;
    private ImageView imageViewBarCode;
    private EditText editTextQuantity;
    private SocketHandler socketHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        findViewById(R.id.buttonOrderItem).setOnClickListener(this);
        findViewById(R.id.buttonSnap).setOnClickListener(this);
        findViewById(R.id.buttonUndo).setOnClickListener(this);
        findViewById(R.id.buttonClear).setOnClickListener(this);

        findViewById(R.id.button1).setOnClickListener(this);
        findViewById(R.id.button5).setOnClickListener(this);
        findViewById(R.id.button10).setOnClickListener(this);
        findViewById(R.id.button50).setOnClickListener(this);
        findViewById(R.id.button100).setOnClickListener(this);
        findViewById(R.id.button500).setOnClickListener(this);

        textViewPartNumber = (TextView)findViewById(R.id.textViewPartNumber);
        imageViewBarCode = (ImageView)findViewById(R.id.imageViewBarCode);
        editTextQuantity = (EditText)findViewById(R.id.editTextQuantity);
        editTextQuantity.setSelection(1);

        qrScan = new IntentIntegrator(this);
        qrScan.setDesiredBarcodeFormats(IntentIntegrator.ONE_D_CODE_TYPES);
        qrScan.setOrientationLocked(false);
        qrScan.setBarcodeImageEnabled(true);
        qrScan.setPrompt("Place a Bar code which contains Item Number to order.");

        socketHandler = SocketHandler.getInstance();

        findViewById(R.id.buttonSnap).callOnClick();
    }

    @Override
    public void onClick(View v) {
        JSONObject obj = new JSONObject();
        switch (v.getId()) {
            case R.id.buttonSnap:
                qrScan.initiateScan();
                break;
            case R.id.buttonClear:
                editTextQuantity.setText("0");
                editTextQuantity.setSelection(1);
                break;
            case R.id.buttonUndo:
//                Toast.makeText(this,"Undo", Toast.LENGTH_SHORT).show();
                if(!socketHandler.isConnected()){
                    Toast.makeText(this, "Socket not connected.", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    obj.put("command", "undo");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonOrderItem).setEnabled(false);
                findViewById(R.id.buttonUndo).setEnabled(false);
                socketHandler.sendOrder(obj.toString(), this);
                break;
            case R.id.buttonOrderItem:
                if(!socketHandler.isConnected()){
                    Toast.makeText(this, "Socket not connected.", Toast.LENGTH_LONG).show();
                    return;
                }
//                Toast.makeText(this,"Undo", Toast.LENGTH_SHORT).show();
                if(Integer.parseInt(editTextQuantity.getText().toString()) <= 0){
                    Toast.makeText(this, "Please input a positive amount.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(textViewPartNumber.getText().toString().length() <= 0){
                    Toast.makeText(this, "Please SNAP a Part Number.", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    obj.put("command", "buy");
                    obj.put("part", textViewPartNumber.getText().toString());
                    obj.put("value", editTextQuantity.getText().toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonOrderItem).setEnabled(false);
                findViewById(R.id.buttonUndo).setEnabled(false);
                findViewById(R.id.buttonSnap).setEnabled(false);
                socketHandler.sendOrder(obj.toString(), this);
                break;
            default:
                Button button = (Button)v;
                int amount = Integer.parseInt(button.getText().toString());
                int current = 0;
                if(editTextQuantity.getText().length() > 0)
                    current = Integer.parseInt(editTextQuantity.getText().toString());
                editTextQuantity.setText(String.valueOf(current + amount));
                editTextQuantity.setSelection(editTextQuantity.getText().length());
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String contents = result.getContents();
            String imagePath = result.getBarcodeImagePath();
            if (contents == null || imagePath == null) {
                Toast.makeText(this, "Result Not Found", Toast.LENGTH_SHORT).show();
            } else {
                textViewPartNumber.setText(contents);
//                Toast.makeText(this, result.getBarcodeImagePath(), Toast.LENGTH_LONG).show();
                imageViewBarCode.setImageBitmap(BitmapFactory.decodeFile(result.getBarcodeImagePath()));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void actionCompleted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                findViewById(R.id.buttonOrderItem).setEnabled(true);
                findViewById(R.id.buttonUndo).setEnabled(true);
                findViewById(R.id.buttonSnap).setEnabled(true);
                ((EditText)findViewById(R.id.editTextQuantity)).setText("0");
                ((EditText)findViewById(R.id.editTextQuantity)).setSelection(1);
            }
        });
    }
}
