package com.hyy.zxing.builder;

import android.content.Context;

import com.hyy.zxing.ui.CaptureActivity;

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


    public ScanBuilder setLayoutRes(int res, OnCustomListener customListener) {
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

    public ScanBuilder setHorizontalScreen(boolean isHorizontal) {
        scanOptions.isHorizontal = isHorizontal;
        return this;
    }

    public ScanBuilder hideBtn(boolean hideBtn) {
        scanOptions.hideBtn = hideBtn;
        return this;
    }

    public ScanBuilder setOnScanListener(OnScanListener scanListener) {
        scanOptions.scanListener = scanListener;
        return this;
    }

    public void build() {
        CaptureActivity.startScan(scanOptions);
    }

}
