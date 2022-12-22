package com.hyy.zxing.builder;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;

import com.hyy.zxing.ui.CaptureActivity;

import java.util.Calendar;

/**
 * @Author : Hou
 * @Time : 2022/12/22 15:22
 * @Description :
 */
public class ScanBuilder {
    private ScanOptions scanOptions;

    //Required
    public ScanBuilder(Context context) {
        scanOptions = new ScanOptions(ScanOptions.SCAN_CONTINUOUS);
        scanOptions.context = context;
    }


    public ScanBuilder setLayoutRes(int res, CustomListener customListener) {
        scanOptions.layoutID = res;
        scanOptions.customListener = customListener;
        return this;
    }

    public ScanBuilder setFull(boolean isFull) {
        scanOptions.isFull = isFull;
        return this;
    }

    public ScanBuilder setMaskColor(int maskColor) {
        scanOptions.maskColor = maskColor;
        return this;
    }

    public ScanBuilder setFrameColor(int frameColor) {
        scanOptions.frameColor = frameColor;
        return this;
    }

    public ScanBuilder setCornerColor(int cornerColor) {
        scanOptions.cornerColor = cornerColor;
        return this;
    }

    public ScanBuilder setLaserColor(int laserColor) {
        scanOptions.laserColor = laserColor;
        return this;
    }


    public void build() {
        CaptureActivity.startScan(scanOptions);
    }

}
