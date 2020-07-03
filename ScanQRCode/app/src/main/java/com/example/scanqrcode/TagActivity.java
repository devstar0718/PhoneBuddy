package com.example.scanqrcode;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
