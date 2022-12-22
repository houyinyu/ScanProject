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
package com.hyy.zxing.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.Result;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hyy.zxing.CaptureHelper;
import com.hyy.zxing.Intents;
import com.hyy.zxing.OnCaptureCallback;
import com.hyy.zxing.R;
import com.hyy.zxing.ViewfinderView;
import com.hyy.zxing.builder.ScanOptions;
import com.hyy.zxing.camera.CameraManager;
import com.hyy.zxing.decode.DecodeImgCallback;
import com.hyy.zxing.decode.DecodeImgThread;
import com.hyy.zxing.decode.ImageUtil;
import com.hyy.zxing.util.Constant;
import com.hyy.zxing.util.LogUtils;

import java.util.List;


/**
 * KEY_REQUEST_TYPE 必传-关系到调用接口
 * KEY_SCAN_TYPE 可传-不传的话就是连扫
 */
public class CaptureActivity extends BaseActivity implements OnCaptureCallback,
        View.OnClickListener {

    public static final String KEY_RESULT = Intents.Scan.RESULT;

    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    //    private View ivTorch;

    private CaptureHelper mCaptureHelper;

    private LinearLayoutCompat scanCode_lightLayout;
    private LinearLayoutCompat scanCode_albumLayout;
    private AppCompatTextView scanCode_lightTv;

    //扫码重启时间时间
    public static final long CODE_RESTART = 1500;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (scanOptions != null && scanOptions.layoutID != 0) {
            setContentView(scanOptions.layoutID);
        } else {
            setContentView(R.layout.layout_zxl_capture);
        }

        initUI();
        mCaptureHelper.onCreate();
    }

    /**
     * 初始化
     */
    public void initUI() {
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


        if (scanOptions != null && scanOptions.maskColor != 0) {
            //遮层罩颜色
            viewfinderView.setMaskColor(scanOptions.maskColor);
        }
        if (scanOptions != null && scanOptions.frameColor != 0) {
            //扫码框边框颜色
            viewfinderView.setFrameColor(scanOptions.frameColor);
        }
        if (scanOptions != null && scanOptions.cornerColor != 0) {
            //扫码框边角颜色
            viewfinderView.setCornerColor(scanOptions.cornerColor);
        }
        if (scanOptions != null && scanOptions.laserColor != 0) {
            //扫描线颜色
            viewfinderView.setLaserColor(scanOptions.laserColor);
        }

        initCaptureHelper();
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.scanCode_lightLayout) {
            /*切换闪光灯*/
            mCaptureHelper.getCameraManager().switchFlashLight(handler);
        } else if (view.getId() == R.id.scanCode_albumLayout) {

            XXPermissions.with(context)
                    // 申请单个权限
                    .permission(Permission.READ_EXTERNAL_STORAGE)
                    // 设置权限请求拦截器（局部设置）
                    //.interceptor(new PermissionInterceptor())
                    // 设置不触发错误检测机制（局部设置）
                    //.unchecked()
                    .request(new OnPermissionCallback() {

                        @Override
                        public void onGranted(@NonNull List<String> permissions, boolean all) {
                            if (!all) {
                                Toast.makeText(context, "获取部分权限成功，但部分权限未正常授予",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            /*打开相册*/
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_PICK);
                            intent.setType("image/*");
                            startActivityForResult(intent, Constant.REQUEST_IMAGE);
                        }

                        @Override
                        public void onDenied(@NonNull List<String> permissions, boolean never) {
                            if (never) {
                                // 如果是被永久拒绝就跳转到应用权限系统设置页面
                                XXPermissions.startPermissionActivity(context, permissions);
                            } else {
                                Toast.makeText(context, "权限获取失败", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });


        }

    }


    /**
     * 初始化扫码配置（设置振动等）
     */
    public void initCaptureHelper() {
        mCaptureHelper = new CaptureHelper(this, surfaceView, viewfinderView);
        if (scanOptions.SCAN_TYPE == ScanOptions.SCAN_CONTINUOUS) {
            mCaptureHelper.continuousScan(true);//连扫
            mCaptureHelper.autoRestartPreviewAndDecode(false);//自动重置扫码器
        }
        mCaptureHelper.playBeep(true);//声音
        mCaptureHelper.vibrate(true);//振动
        mCaptureHelper.supportAutoZoom(true);//自动缩放
        mCaptureHelper.supportLuminanceInvert(true);//支持识别反色码，黑白颜色反转
        mCaptureHelper.supportVerticalCode(true);//支持扫垂直的条码
        //        mCaptureHelper.supportZoom(true);
        if (scanOptions != null && scanOptions.isFull) {
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
        LogUtils.e("******************扫码返回:" + result);
        mCaptureHelper.autoRestartPreviewAndDecode(false);//自动重置扫码器
        //扫码类别
        requestScanType(result);
        //重启扫码
        //        mHandler.sendEmptyMessageDelayed(1, CODE_RESTART);
        return scanOptions.SCAN_TYPE == ScanOptions.SCAN_CONTINUOUS;
    }

    /**
     * 处理进入扫码界面的目的
     *
     * @param result
     */
    private void requestScanType(String result) {
        finish();
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
                    Toast.makeText(CaptureActivity.this, R.string.scan_failed_tip,
                            Toast.LENGTH_SHORT).show();
                }
            }).run();

        }
    }

    private static ScanOptions scanOptions;

    public static void startScan(ScanOptions options) {
        scanOptions = options;
        XXPermissions.with(options.context)
                // 申请单个权限
                .permission(Permission.CAMERA)
                // 设置权限请求拦截器（局部设置）
                //.interceptor(new PermissionInterceptor())
                // 设置不触发错误检测机制（局部设置）
                //.unchecked()
                .request(new OnPermissionCallback() {

                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean all) {
                        if (!all) {
                            Toast.makeText(options.context, "获取部分权限成功，但部分权限未正常授予",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent intent = new Intent(options.context, CaptureActivity.class);
                        options.context.startActivity(intent);
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean never) {
                        if (never) {
                            // 如果是被永久拒绝就跳转到应用权限系统设置页面
                            XXPermissions.startPermissionActivity(options.context, permissions);
                        } else {
                            Toast.makeText(options.context, "权限获取失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}