/*
 * Copyright (C) 2018 Jenly Yu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyy.zxing;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.google.zxing.Result;
import com.hyy.zxing.CaptureHelper;
import com.hyy.zxing.Intents;
import com.hyy.zxing.OnCaptureCallback;
import com.hyy.zxing.ViewfinderView;
import com.hyy.zxing.camera.CameraManager;
import com.hyy.zxing.decode.DecodeImgCallback;
import com.hyy.zxing.decode.DecodeImgThread;
import com.hyy.zxing.decode.ImageUtil;
import com.hyy.zxing.util.Constant;
import com.hyy.zxing.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;


/**
 * KEY_REQUEST_TYPE 必传-关系到调用接口
 * KEY_SCAN_TYPE 可传-不传的话就是连扫
 */
public class CaptureActivity extends BaseActivity implements OnCaptureCallback, View.OnClickListener {

    public static final String KEY_RESULT = Intents.Scan.RESULT;

    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
//    private View ivTorch;

    private CaptureHelper mCaptureHelper;

    private LinearLayoutCompat scanCode_lightLayout;
    private LinearLayoutCompat scanCode_albumLayout;
    private AppCompatTextView scanCode_lightTv;

    //手动输入条码的请求码
    public static final int REQUEST_INPUT_CODE = 1234;

    //下面一堆事选择连扫还是单扫的
    public static final String KEY_SCAN_TYPE = "KEY_SCAN_TYPE";
    public static final int KEY_SCAN_SINGLE = 1;
    public static final int KEY_SCAN_CONTINUOUS = 2;
    private int SCAN_TYPE = 2;

    //扫码请求接口类型
    public static final String KEY_REQUEST_TYPE = "KEY_REQUEST_TYPE";
    public static final String KEY_REQUEST_RETURN = "request_return_scan";//直接返回条形码string
    public static final String KEY_REQUEST_SHOP_CART = "request_shop_cart";//商城端加入购物车
    public static final String KEY_REQUEST_PRESENT = "request_present";//管理端商品提报新增时候扫码，会跳转到详情
    public static final String KEY_REQUEST_PLACE_ORDER = "request_place_order";//管理端代下单
    public static final String KEY_REQUEST_TOURIST_ORDER = "request_tourist_order";//管理游客下单
    public static final String KEY_REQUEST_RETAIL_ORDER = "request_retail_order";//管理端零售下单
    public static final String KEY_REQUEST_OTHER_STOCK = "request_other_stock";//管理端其他入库-扫码入库
    public static final String KEY_REQUEST_BIND_MERCHANT = "request_bind_merchant";//登录扫描绑定商户使用
    private String SCAN_REQUEST_TYPE = KEY_REQUEST_RETURN;

    //扫码重启时间时间
    public static final long CODE_RESTART = 1500;

    //只有返回条形码才有（在商品提报界面，列表里面有扫码，这里加上position，是返回的时候，能设置列表数据）
    private int parentPos;
    private int childPos;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_zxl_capture);
        initUI();
        mCaptureHelper.onCreate();
    }

    /**
     * 初始化
     */
    public void initUI() {
        Intent intent = getIntent();
        if (intent != null) {
            SCAN_TYPE = intent.getIntExtra(KEY_SCAN_TYPE, 2);
            SCAN_REQUEST_TYPE = intent.getStringExtra(KEY_REQUEST_TYPE);
        }
        surfaceView = findViewById(getSurfaceViewId());
        int viewfinderViewId = getViewfinderViewId();
        if (viewfinderViewId != 0) {
            viewfinderView = findViewById(viewfinderViewId);
        }
        scanCode_lightLayout = findViewById(R.id.scanCode_lightLayout);
        scanCode_lightTv = findViewById(R.id.scanCode_lightTv);
        scanCode_albumLayout = findViewById(R.id.scanCode_albumLayout);

        scanCode_lightLayout.setOnClickListener(this);
        scanCode_albumLayout.setOnClickListener(this);


        /*有闪光灯就显示手电筒按钮  否则不显示*/
        if (isSupportCameraLedFlash(getPackageManager())) {
            scanCode_lightLayout.setVisibility(View.VISIBLE);
        } else {
            scanCode_lightLayout.setVisibility(View.GONE);
        }

        initCaptureHelper();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.scanCode_lightLayout:
                /*切换闪光灯*/
                mCaptureHelper.getCameraManager().switchFlashLight(handler);
                break;
            case R.id.scanCode_albumLayout:
                /*打开相册*/
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_PICK);
//                intent.setType("image/*");
//                startActivityForResult(intent, Constant.REQUEST_IMAGE);
                break;
            default:
        }
    }

    private boolean isFull = true;

    /**
     * 初始化扫码配置（设置振动等）
     */
    public void initCaptureHelper() {
        mCaptureHelper = new CaptureHelper(this, surfaceView, viewfinderView);
        if (SCAN_TYPE == KEY_SCAN_CONTINUOUS) {
            mCaptureHelper.continuousScan(true);//连扫
            mCaptureHelper.autoRestartPreviewAndDecode(false);//自动重置扫码器
        }
        mCaptureHelper.playBeep(true);//声音
        mCaptureHelper.vibrate(true);//振动
        mCaptureHelper.supportAutoZoom(true);//自动缩放
        mCaptureHelper.supportLuminanceInvert(true);//支持识别反色码，黑白颜色反转
        mCaptureHelper.supportVerticalCode(true);//支持扫垂直的条码
//        mCaptureHelper.supportZoom(true);
        if (isFull) {
            mCaptureHelper.fullScreenScan(true);
            //全屏扫描的话就修改UI
            viewfinderView.setFullScreenScan(true);
        }
        mCaptureHelper.setOnCaptureCallback(this);
    }


    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (handler != null) {
                switchFlashImg(msg.what);
                return true;
            }
            return false;
        }
    });


    /**
     * @param pm
     * @return 是否有闪光灯
     */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param flashState 切换闪光灯图片
     */
    public void switchFlashImg(int flashState) {
        if (flashState == Constant.FLASH_OPEN) {
            scanCode_lightTv.setBackgroundResource(R.drawable.icon_scan_light_on);
        } else {
            scanCode_lightTv.setBackgroundResource(R.drawable.icon_scan_light_off);
        }
    }

    /**
     * {@link #viewfinderView} 的 ID
     *
     * @return 默认返回{@code R.id.viewfinderView}, 如果不需要扫码框可以返回0
     */
    public int getViewfinderViewId() {
        return R.id.viewfinderView;
    }


    /**
     * 预览界面{@link #surfaceView} 的ID
     *
     * @return
     */
    public int getSurfaceViewId() {
        return R.id.surfaceView;
    }

    /**
     * 获取 {@link #ivTorch} 的ID
     * @return 默认返回{@code R.id.ivTorch}, 如果不需要手电筒按钮可以返回0
     */
//    public int getIvTorchId(){
//        return R.id.ivTorch;
//    }

    /**
     * Get {@link CaptureHelper}
     *
     * @return {@link #mCaptureHelper}
     */
    public CaptureHelper getCaptureHelper() {
        return mCaptureHelper;
    }

    /**
     * Get {@link CameraManager} use {@link #getCaptureHelper()#getCameraManager()}
     *
     * @return {@link #mCaptureHelper#getCameraManager()}
     */
    @Deprecated
    public CameraManager getCameraManager() {
        return mCaptureHelper.getCameraManager();
    }

    @Override
    public void onResume() {
        super.onResume();
        mCaptureHelper.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mCaptureHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCaptureHelper.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mCaptureHelper.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    /**
     * 接收扫码结果回调
     *
     * @param result 扫码结果
     * @return 返回true表示拦截，将不自动执行后续逻辑，为false表示不拦截，默认不拦截
     */
    @Override
    public boolean onResultCallback(String result) {
        LogUtils.i("******************扫码返回:" + result);
        mCaptureHelper.autoRestartPreviewAndDecode(false);//自动重置扫码器
        //扫码类别
        requestScanType(result);
        //重启扫码
//        mHandler.sendEmptyMessageDelayed(1, CODE_RESTART);
        return SCAN_TYPE == KEY_SCAN_CONTINUOUS;
    }

    /**
     * 处理进入扫码界面的目的
     *
     * @param result
     */
    private void requestScanType(String result) {

    }


    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 1:
                    //扫码重启延时
                    mCaptureHelper.restartPreviewAndDecode();
                    break;
                default:
            }
            return false;
        }
    });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());
            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    mCaptureHelper.onResult(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureActivity.this, R.string.scan_failed_tip, Toast.LENGTH_SHORT).show();
                }
            }).run();

        }
    }
}