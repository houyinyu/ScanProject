package com.hyy.project;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hyy.zxing.builder.OnCustomListener;
import com.hyy.zxing.builder.OnScanListener;
import com.hyy.zxing.builder.ScanBuilder;
import com.hyy.zxing.ui.CaptureActivity;
import com.hyy.zxing.util.LogUtils;

public class MainActivity extends AppCompatActivity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        TextView startScan = findViewById(R.id.startScan);
        startScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ScanBuilder scanBuilder = new ScanBuilder(context);
                scanBuilder.setOnScanListener(new OnScanListener() {
                    @Override
                    public void onResult(String qrCode) {
                        //返回二维码
                        LogUtils.i("result：" + qrCode);
                    }
                });
                scanBuilder.setLayoutRes(R.layout.layout_zxl_capture2, new OnCustomListener() {
                    @Override
                    public void customLayout(Activity activity) {
                        //自定义布局需要添加指定ID：
                        // R.id.surfaceView
                        // R.id.viewfinderView
                        // R.id.scanCode_lightLayout
                        // R.id.scanCode_lightTv
                        // R.id.scanCode_albumLayout
                        ImageView imageView = activity.findViewById(R.id.scanCode_back);
                    }
                });
                scanBuilder.setCornerColor(Color.parseColor("#E40404")).build();
            }
        });
    }
}