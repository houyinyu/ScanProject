# ScanProject
扫码

## 使用方式

```Java
      implementation 'com.github.houyinyu:ScanProject:1.0'
```
```Java
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
           // R.id.scanCode_back
           ImageView imageView = activity.findViewById(R.id.scanCode_back);
            }
        });
    scanBuilder.setCornerColor(Color.parseColor("#E40404")).build();
```
