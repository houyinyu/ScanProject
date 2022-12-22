package com.hyy.project;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.hyy.zxing.builder.ScanBuilder;
import com.hyy.zxing.ui.CaptureActivity;

public class MainActivity extends AppCompatActivity {

    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context=this;
        TextView startScan=findViewById(R.id.startScan);
        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanBuilder scanBuilder=new ScanBuilder(context);
                scanBuilder.setCornerColor(Color.parseColor("#E40404")).build();
            }
        });
    }
}