package com.hyy.zxing.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;


import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class BaseActivity extends AppCompatActivity {
    public Context context;


    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }


    public boolean notEmpty(Object val) {
        return !isEmpty(val);
    }

    public boolean isEmpty(Object obj) {
        if (obj == null) return true;
        if (obj instanceof String && obj.toString().length() <= 0) return true;
        if (obj.getClass().isArray() && Array.getLength(obj) <= 0) return true;
        if (obj instanceof Collection && ((Collection) obj).isEmpty()) return true;
        if (obj instanceof Map && ((Map) obj).isEmpty()) return true;
        if (obj instanceof SparseArray && ((SparseArray) obj).size() <= 0) return true;
        if (obj instanceof SparseBooleanArray && ((SparseBooleanArray) obj).size() <= 0)
            return true;
        if (obj instanceof SparseIntArray && ((SparseIntArray) obj).size() <= 0) return true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (obj instanceof SparseLongArray && ((SparseLongArray) obj).size() <= 0) return true;
        }
        return false;
    }

    public void toast(String msg) {
        if (context == null) return;
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * 启动其他Activity
     *
     * @param cls 需要启动的那个Activity
     */
    public void goIntent(Class<?> cls) {
        Intent intent = new Intent(this, cls);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
        }
    }

    /**
     * 启动其他Activity
     *
     * @param cls  需要启动的那个Activity
     * @param flag 启动模式
     */
    public void goIntent(Class<?> cls, int flag) {
        goIntent(cls, flag, null);
    }

    /**
     * 启动其他Activity
     *
     * @param cls    需要启动的那个Activity
     * @param flag   启动模式
     * @param bundle 数据
     */
    public void goIntent(Class<?> cls, int flag, Bundle bundle) {
        Intent intent = new Intent(this, cls);
        intent.addFlags(flag);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
        }
    }

    /**
     * 启动其他Activity
     *
     * @param cls    需要启动的那个Activity
     * @param bundle 数据
     */
    public void goIntent(Class<?> cls, Bundle bundle) {
        Intent intent = new Intent(this, cls);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
        }
    }

    /**
     * 启动其他Activity
     *
     * @param cls       需要启动的那个Activity
     * @param ext_key   数据的key
     * @param ext_value 数据
     */
    public void goIntent(Class<?> cls, String ext_key, String ext_value) {
        Intent intent = new Intent(this, cls);
        if (ext_value != null && ext_key != null) {
            intent.putExtra(ext_key, ext_value);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
        }
    }

    public void goIntent(Class<?> cls, String ext_key, int ext_value) {
        Intent intent = new Intent(this, cls);
        intent.putExtra(ext_key, ext_value);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
        }
    }

    public void goIntentResult(Class<?> cls, int requestCode) {
        Intent intent = new Intent(this, cls);
        startActivityForResult(intent, requestCode);
    }

    public void goIntentResult(Class<?> cls, int requestCode, String key, String data) {
        Intent intent = new Intent(this, cls);
        intent.putExtra(key, data);
        startActivityForResult(intent, requestCode);
    }



    @Override
    public void finish() {
        super.finish();
        hideKeyBoard();
    }


    /**
     * 返回桌面
     */
    private long mCurrentTime = 0; // 当前时间毫秒值(退出的时候使用)

    public void onKeyBack() {
        if ((System.currentTimeMillis() - mCurrentTime > 1500)) {
            toast("再按一次返回桌面");
            mCurrentTime = System.currentTimeMillis();
        } else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        }
    }

    /**
     * 打开软键盘
     *
     * @param view
     */
    public void showKeyBoard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            //使editText获取焦点
            view.requestFocus();
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED); // 强制打开软键盘
        }
    }

    /**
     * 关闭软键盘
     */
    public void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
        }
    }


}
