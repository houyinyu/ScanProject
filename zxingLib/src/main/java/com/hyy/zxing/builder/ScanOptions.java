package com.hyy.zxing.builder;

import android.content.Context;

import com.hyy.zxing.R;

/**
 * @Author : Hou
 * @Time : 2022/12/22 15:23
 * @Description :
 */
public class ScanOptions {
    public Context context;
    public int layoutID = 0;//自定义view
    public boolean isFull = false;//是否全屏
    public int maskColor = R.color.viewfinder_mask;//遮层罩颜色
    public int frameColor = R.color.viewfinder_frame;//扫码框颜色（全屏不显示）
    public int cornerColor = R.color.viewfinder_corner;//扫码框四个角的颜色（全屏不显示）
    public int laserColor = R.color.viewfinder_laser;//扫描线颜色
    public int cornerWidth = 0;//扫码框四个角的宽（粗细）
    public int cornerHeight = 0;//扫码框四个角的高（长短）
    public boolean hideBtn = false;//隐藏闪光灯和相册
    public boolean hideBack = false;//隐藏返回按钮
    public boolean isHorizontal = false;//是否强制横屏

    public OnCustomListener customListener;
    public OnScanListener scanListener;


    //下面一堆事选择连扫还是单扫的
    public static final int SCAN_SINGLE = 1;//单扫
    public static final int SCAN_CONTINUOUS = 2;//连扫
    public int SCAN_TYPE = SCAN_CONTINUOUS;

    public ScanOptions(int SCAN_TYPE) {
        this.SCAN_TYPE = SCAN_TYPE;
    }
}
