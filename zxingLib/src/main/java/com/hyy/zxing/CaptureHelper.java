/*
 * Copyright (C) 2019 Jenly Yu
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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.hyy.zxing.camera.CameraManager;
import com.hyy.zxing.camera.FrontLightMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.FloatRange;
import androidx.fragment.app.Fragment;

/**
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class CaptureHelper implements CaptureLifecycle, CaptureTouchEvent,CaptureManager, SurfaceHolder.Callback  {

    private Activity activity;

    private CaptureHandler captureHandler;
    private OnCaptureListener onCaptureListener;

    private CameraManager cameraManager;

    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;


    private SurfaceView surfaceView;
    private ViewfinderView viewfinderView;
    private SurfaceHolder surfaceHolder;
    private View ivTorch;

    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType,Object> decodeHints;
    private String characterSet;

    private boolean hasSurface;
    /**
     * ?????????????????????
     */
    private static final int DEVIATION = 6;
    /**
     * ?????????????????????????????????????????????
     */
    private boolean isSupportZoom = true;
    private float oldDistance;

    /**
     * ???????????????????????????????????????????????????
     */
    private boolean isSupportAutoZoom = true;

    /**
     * ??????????????????????????????????????????????????????????????????????????????
     */
    private boolean isSupportLuminanceInvert = false;

    /**
     * ????????????????????????????????????
     */
    private boolean isContinuousScan = false;
    /**
     * ?????????????????????????????????????????????????????????????????????
     */
    private boolean isAutoRestartPreviewAndDecode = true;
    /**
     * ??????????????????
     */
    private boolean isPlayBeep;
    /**
     * ????????????
     */
    private boolean isVibrate;

    /**
     * ??????????????????????????????
     */
    private boolean isSupportVerticalCode;

    /**
     * ????????????????????????
     */
    private boolean isReturnBitmap;

    /**
     * ??????????????????????????????
     */
    private boolean isFullScreenScan;

    /**
     * ????????????????????????????????????0.625 ~ 1.0???????????????0.9
     */
    private float framingRectRatio = 0.9f;
    /**
     * ?????????????????????????????????
     */
    private int framingRectVerticalOffset;
    /**
     * ?????????????????????????????????
     */
    private int framingRectHorizontalOffset;
    /**
     * ????????????????????????????????????????????????????????????????????????????????????
     */
    private float tooDarkLux =AmbientLightManager.TOO_DARK_LUX;
    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????
     */
    private float brightEnoughLux = AmbientLightManager.BRIGHT_ENOUGH_LUX;
    /**
     * ????????????
     */
    private OnCaptureCallback onCaptureCallback;

    private boolean hasCameraFlash;

    /**
     * use {@link #CaptureHelper(Fragment, SurfaceView, ViewfinderView, View)}
     * @param fragment
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated
    public CaptureHelper(Fragment fragment, SurfaceView surfaceView, ViewfinderView viewfinderView){
        this(fragment,surfaceView,viewfinderView,null);
    }

    public CaptureHelper(Fragment fragment, SurfaceView surfaceView, ViewfinderView viewfinderView,View ivTorch){
        this(fragment.getActivity(),surfaceView,viewfinderView,ivTorch);
    }

    /**
     *  use {@link #CaptureHelper(Activity, SurfaceView, ViewfinderView, View)}
     * @param activity
     * @param surfaceView
     * @param viewfinderView
     */
    @Deprecated
    public CaptureHelper(Activity activity,SurfaceView surfaceView,ViewfinderView viewfinderView){
        this(activity,surfaceView,viewfinderView,null);
    }

    /**
     *
     * @param activity
     * @param surfaceView
     * @param viewfinderView
     * @param ivTorch
     */
    public CaptureHelper(Activity activity,SurfaceView surfaceView,ViewfinderView viewfinderView,View ivTorch){
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.viewfinderView = viewfinderView;
        this.ivTorch = ivTorch;
    }


    @Override
    public void onCreate(){
        surfaceHolder = surfaceView.getHolder();
        hasSurface = false;
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
        ambientLightManager = new AmbientLightManager(activity);

        hasCameraFlash = activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        initCameraManager();

        onCaptureListener = (result, barcode, scaleFactor) -> {
            inactivityTimer.onActivity();
            onResult(result,barcode,scaleFactor);
        };
        //?????????????????????????????????
        beepManager.setPlayBeep(isPlayBeep);
        beepManager.setVibrate(isVibrate);

        //??????????????????????????????????????????????????????
        ambientLightManager.setTooDarkLux(tooDarkLux);
        ambientLightManager.setBrightEnoughLux(brightEnoughLux);

    }



    @Override
    public void onResume(){
        beepManager.updatePrefs();

        inactivityTimer.onResume();

        if (hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
        }
        ambientLightManager.start(cameraManager);
    }


    @Override
    public void onPause(){
        if (captureHandler != null) {
            captureHandler.quitSynchronously();
            captureHandler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        cameraManager.closeDriver();
        if (!hasSurface) {
            surfaceHolder.removeCallback(this);
        }
        if(ivTorch != null && ivTorch.getVisibility() == View.VISIBLE){
            ivTorch.setSelected(false);
            ivTorch.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void onDestroy(){
        inactivityTimer.shutdown();
    }

    /**
     * ????????????????????????{@link Activity#onTouchEvent(MotionEvent)}???????????????
     * @param event
     */
    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(isSupportZoom && cameraManager.isOpen()){
            Camera camera = cameraManager.getOpenCamera().getCamera();
            if(camera ==null){
                return false;
            }
            if(event.getPointerCount() > 1) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {//????????????
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDistance = calcFingerSpacing(event);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newDistance = calcFingerSpacing(event);

                        if (newDistance > oldDistance + DEVIATION) {//
                            handleZoom(true, camera);
                        } else if (newDistance < oldDistance - DEVIATION) {
                            handleZoom(false, camera);
                        }
                        oldDistance = newDistance;
                        break;
                }

                return true;
            }
        }

        return false;
    }

    private void initCameraManager(){
        cameraManager = new CameraManager(activity);
        cameraManager.setFullScreenScan(isFullScreenScan);
        cameraManager.setFramingRectRatio(framingRectRatio);
        cameraManager.setFramingRectVerticalOffset(framingRectVerticalOffset);
        cameraManager.setFramingRectHorizontalOffset(framingRectHorizontalOffset);
        if(ivTorch !=null && hasCameraFlash){
            ivTorch.setOnClickListener(v -> {
                if(cameraManager!=null){
                    cameraManager.setTorch(!ivTorch.isSelected());
                }
            });
            cameraManager.setOnSensorListener((torch, tooDark, ambientLightLux) -> {
                if(tooDark){
                    if(ivTorch.getVisibility() != View.VISIBLE){
                        ivTorch.setVisibility(View.VISIBLE);
                    }
                }else if(!torch){
                    if(ivTorch.getVisibility() == View.VISIBLE){
                        ivTorch.setVisibility(View.INVISIBLE);
                    }
                }
            });
            cameraManager.setOnTorchListener(torch -> ivTorch.setSelected(torch));

        }
    }


    /**
     * ?????????Camera
     * @param surfaceHolder
     */
    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            com.hyy.zxing.util.LogUtils.w("initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (captureHandler == null) {
                captureHandler = new CaptureHandler(activity,viewfinderView,onCaptureListener, decodeFormats, decodeHints, characterSet, cameraManager);
                captureHandler.setSupportVerticalCode(isSupportVerticalCode);
                captureHandler.setReturnBitmap(isReturnBitmap);
                captureHandler.setSupportAutoZoom(isSupportAutoZoom);
                captureHandler.setSupportLuminanceInvert(isSupportLuminanceInvert);
            }
        } catch (IOException ioe) {
            com.hyy.zxing.util.LogUtils.w( ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            com.hyy.zxing.util.LogUtils.w( "Unexpected error initializing camera", e);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            com.hyy.zxing.util.LogUtils.w( "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    /**
     * ??????????????????
     * @param isZoomIn
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            com.hyy.zxing.util.LogUtils.i( "zoom not supported");
        }
    }

    /**
     * ??????
     * @param event
     * @param camera
     */
    @Deprecated
    private void focusOnTouch(MotionEvent event,Camera camera) {

        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();

        Rect focusRect = calcTapArea(event.getRawX(), event.getRawY(), 1f,previewSize);
        Rect meteringRect = calcTapArea(event.getRawX(), event.getRawY(), 1.5f,previewSize);
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 600));
            parameters.setFocusAreas(focusAreas);
        }

        if (parameters.getMaxNumMeteringAreas() > 0) {
            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(meteringRect, 600));
            parameters.setMeteringAreas(meteringAreas);
        }
        final String currentFocusMode = params.getFocusMode();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        camera.setParameters(params);

        camera.autoFocus((success, camera1) -> {
            Camera.Parameters params1 = camera1.getParameters();
            params1.setFocusMode(currentFocusMode);
            camera1.setParameters(params1);
        });

    }


    /**
     * ?????????????????????
     * @param event
     * @return
     */
    private float calcFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * ??????????????????
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private Rect calcTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 200;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) ((x / previewSize.width) * 2000 - 1000);
        int centerY = (int) ((y / previewSize.height) * 2000 - 1000);
        int left = clamp(centerX - (areaSize / 2), -1000, 1000);
        int top = clamp(centerY - (areaSize / 2), -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top),
                Math.round(rectF.right), Math.round(rectF.bottom));
    }

    /**
     * ?????????????????????
     * @param x
     * @param min ???????????????
     * @param max ???????????????
     * @return
     */
    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }


    /**
     * ??????????????????????????????
     */
    public void restartPreviewAndDecode(){
        if(captureHandler!=null){
            captureHandler.restartPreviewAndDecode();
        }
    }

    /**
     * ??????????????????
     * @param result
     * @param barcode
     * @param scaleFactor
     */
    public void onResult(Result result, Bitmap barcode, float scaleFactor){
        onResult(result);
    }

    /**';, mnb
     *
     * ????????????????????????????????????????????????{@link #continuousScan(boolean)}?????????{@code true}
     * ??????{@link #isContinuousScan}?????????????????????????????????????????????????????????????????????????????????
     * ??????{@link #autoRestartPreviewAndDecode(boolean)}?????????{@code false}??????????????????{@link #restartPreviewAndDecode()}
     * @param result ????????????
     */
    public void onResult(Result result){
        beepManager.playBeepSoundAndVibrate();

        final String text = result.getText();
        if(isContinuousScan){
            if(onCaptureCallback!=null){
                onCaptureCallback.onResultCallback(text);
            }
            if(isAutoRestartPreviewAndDecode){
                restartPreviewAndDecode();
            }
            return;
        }

        if(isPlayBeep && captureHandler != null){//?????????????????????????????????????????????????????????????????????
            captureHandler.postDelayed(() -> {
                //??????????????????????????????onCallback?????????true??????????????????
                if(onCaptureCallback!=null && onCaptureCallback.onResultCallback(text)){
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra(Intents.Scan.RESULT,text);
                activity.setResult(Activity.RESULT_OK,intent);
                activity.finish();
            },100);
            return;
        }

        //??????????????????????????????onCallback?????????true??????????????????
        if(onCaptureCallback!=null && onCaptureCallback.onResultCallback(text)){
            return;
        }
        Intent intent = new Intent();
        intent.putExtra(Intents.Scan.RESULT,text);
        activity.setResult(Activity.RESULT_OK,intent);
        activity.finish();
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????{@code true}?????????{@link #onResult(Result)}
     */
    public CaptureHelper continuousScan(boolean isContinuousScan){
        this.isContinuousScan = isContinuousScan;
        return this;
    }


    /**
     * ??????????????????????????????????????????????????????????????????????????????
     * @return ???????????? true
     */
    public CaptureHelper autoRestartPreviewAndDecode(boolean isAutoRestartPreviewAndDecode){
        this.isAutoRestartPreviewAndDecode = isAutoRestartPreviewAndDecode;
        return this;
    }


    /**
     * ????????????????????????
     * @return
     */
    public CaptureHelper playBeep(boolean playBeep){
        this.isPlayBeep = playBeep;
        if(beepManager!=null){
            beepManager.setPlayBeep(playBeep);
        }
        return this;
    }

    /**
     * ??????????????????
     * @return
     */
    public CaptureHelper vibrate(boolean vibrate){
        this.isVibrate = vibrate;
        if(beepManager!=null){
            beepManager.setVibrate(vibrate);
        }
        return this;
    }


    /**
     * ????????????????????????
     * @param supportZoom
     * @return
     */
    public CaptureHelper supportZoom(boolean supportZoom) {
        isSupportZoom = supportZoom;
        return this;
    }

    /**
     * ????????????????????????/?????????????????????????????????????????????
     * @param decodeFormats  ?????????{@link DecodeFormatManager}
     * @return
     */
    public CaptureHelper decodeFormats(Collection<BarcodeFormat> decodeFormats) {
        this.decodeFormats = decodeFormats;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param decodeHints
     * @return
     */
    public CaptureHelper decodeHints(Map<DecodeHintType,Object> decodeHints) {
        this.decodeHints = decodeHints;
        return this;
    }

    /**
     * {@link DecodeHintType}
     * @param key {@link DecodeHintType}
     * @param value {@link }
     * @return
     */
    public CaptureHelper decodeHint(DecodeHintType key,Object value){
        if(decodeHints == null){
            decodeHints = new EnumMap<>(DecodeHintType.class);
        }
        decodeHints.put(key,value);
        return this;
    }

    /**
     *  ??????????????????????????????
     * @param characterSet
     * @return
     */
    public CaptureHelper characterSet(String characterSet) {
        this.characterSet = characterSet;
        return this;
    }

    /**
     * ????????????????????????????????????
     * @param supportVerticalCode ?????????false?????????????????????????????????????????????????????????????????????????????????
     * @return
     */
    public CaptureHelper supportVerticalCode(boolean supportVerticalCode) {
        this.isSupportVerticalCode = supportVerticalCode;
        if(captureHandler!=null){
            captureHandler.setSupportVerticalCode(isSupportVerticalCode);
        }
        return this;
    }

    /**
     * ?????????????????????????????????????????????{@link FrontLightMode#AUTO}?????????????????????????????????????????????
     * ?????????{@link #tooDarkLux(float)}???{@link #brightEnoughLux(float)}???????????????????????????
     * ?????????????????????????????????????????????
     * ??????????????????{@link FrontLightMode#AUTO}???????????????????????????????????????????????????
     *
     * @param mode ??????:{@link FrontLightMode#AUTO}
     * @return
     */
    public CaptureHelper frontLightMode(FrontLightMode mode) {
        com.hyy.zxing.camera.FrontLightMode.put(activity,mode);
        if(ivTorch!=null && mode != com.hyy.zxing.camera.FrontLightMode.AUTO){
            ivTorch.setVisibility(View.INVISIBLE);
        }
        return this;
    }

    /**
     * ???????????????????????????????????????????????????
     * @param tooDarkLux  ?????????{@link AmbientLightManager#TOO_DARK_LUX}
     * @return
     */
    public CaptureHelper tooDarkLux(float tooDarkLux) {
        this.tooDarkLux = tooDarkLux;
        if(ambientLightManager != null){
            ambientLightManager.setTooDarkLux(tooDarkLux);
        }
        return this;
    }

    /**
     * ?????????????????????????????????????????????????????????
     * @param brightEnoughLux ?????????{@link AmbientLightManager#BRIGHT_ENOUGH_LUX}
     * @return
     */
    public CaptureHelper brightEnoughLux(float brightEnoughLux) {
        this.brightEnoughLux = brightEnoughLux;
        if(ambientLightManager != null){
            ambientLightManager.setTooDarkLux(tooDarkLux);
        }
        return this;
    }

    /**
     * ????????????????????????
     * @param returnBitmap ?????????false????????????true??????????????????????????????????????????????????????????????????????????????
     * @return
     */
    public CaptureHelper returnBitmap(boolean returnBitmap) {
        isReturnBitmap = returnBitmap;
        if(captureHandler!=null){
            captureHandler.setReturnBitmap(isReturnBitmap);
        }
        return this;
    }


    /**
     * ??????????????????????????????
     * @param supportAutoZoom
     * @return
     */
    public CaptureHelper supportAutoZoom(boolean supportAutoZoom) {
        isSupportAutoZoom = supportAutoZoom;
        if(captureHandler!=null){
            captureHandler.setSupportAutoZoom(isSupportAutoZoom);
        }
        return this;
    }

    /**
     * ????????????????????????????????????????????????
     * @param supportLuminanceInvert ?????????false????????????true??????????????????????????????????????????????????????????????????????????????
     * @return
     */
    public CaptureHelper supportLuminanceInvert(boolean supportLuminanceInvert) {
        isSupportLuminanceInvert = supportLuminanceInvert;
        if(captureHandler!=null){
            captureHandler.setSupportLuminanceInvert(isSupportLuminanceInvert);
        }
        return this;
    }

    /**
     * ????????????????????????????????????
     * @param fullScreenScan ?????????false
     * @return
     */
    public CaptureHelper fullScreenScan(boolean fullScreenScan) {
        isFullScreenScan = fullScreenScan;
        if(cameraManager!=null){
            cameraManager.setFullScreenScan(isFullScreenScan);
        }
        return this;
    }

    /**
     * ??????????????????????????????????????????0.625 ~ 1.0????????????????????????????????????
     * 0.625 ???????????????????????????????????????1.0?????????????????????
     * @param framingRectRatio ??????0.9
     * @return
     */
    public CaptureHelper framingRectRatio(@FloatRange(from = 0.0f ,to = 1.0f) float framingRectRatio) {
        this.framingRectRatio = framingRectRatio;
        if(cameraManager!=null){
            cameraManager.setFramingRectRatio(framingRectRatio);
        }
        return this;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     * @param framingRectVerticalOffset ??????0??????????????????
     * @return
     */
    public CaptureHelper framingRectVerticalOffset(int framingRectVerticalOffset) {
        this.framingRectVerticalOffset = framingRectVerticalOffset;
        if(cameraManager!=null){
            cameraManager.setFramingRectVerticalOffset(framingRectVerticalOffset);
        }
        return this;
    }

    /**
     * ?????????????????????????????????????????????????????????????????????
     * @param framingRectHorizontalOffset ??????0??????????????????
     * @return
     */
    public CaptureHelper framingRectHorizontalOffset(int framingRectHorizontalOffset) {
        this.framingRectHorizontalOffset = framingRectHorizontalOffset;
        if(cameraManager!=null){
            cameraManager.setFramingRectHorizontalOffset(framingRectHorizontalOffset);
        }
        return this;
    }


    /**
     * ??????????????????
     * @param callback
     * @return
     */
    public CaptureHelper setOnCaptureCallback(OnCaptureCallback callback) {
        this.onCaptureCallback = callback;
        return this;
    }

    /**
     * {@link CameraManager}
     * @return {@link #cameraManager}
     */
    @Override
    public CameraManager getCameraManager() {
        return cameraManager;
    }

    /**
     * {@link BeepManager}
     * @return {@link #beepManager}
     */
    @Override
    public BeepManager getBeepManager() {
        return beepManager;
    }

    /**
     * {@link AmbientLightManager}
     * @return {@link #ambientLightManager}
     */
    @Override
    public AmbientLightManager getAmbientLightManager() {
        return ambientLightManager;
    }

    /**
     * {@link InactivityTimer}
     * @return {@link #inactivityTimer}
     */
    @Override
    public InactivityTimer getInactivityTimer() {
        return inactivityTimer;
    }
}