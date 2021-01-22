package com.example.sensor;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DialogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        final Handler handler = new Handler();
        final Dialog dialog = new Dialog(DialogActivity.this);

        dialog.setContentView(R.layout.popup_dialog);
        Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //handler.removeCallbacksAndMessages(null);
                setResult(1001);
                dialog.dismiss();
                finish();
            }
        });
        dialog.show();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setResult(1001);
                dialog.dismiss();
                finish();
            }
        }, 7000);
    }
}
