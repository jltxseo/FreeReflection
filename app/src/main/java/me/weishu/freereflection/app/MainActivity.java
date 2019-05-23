package me.weishu.freereflection.app;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import me.weishu.reflection.Reflection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "version : " + Build.VERSION.SDK_INT + " fingerprint: " + Build.FINGERPRINT);
        testHidden();
        findViewById(R.id.test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testHidden();
            }
        });

        findViewById(R.id.unreal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int ret = Reflection.unseal(MainActivity.this);
                toast("unseal result: " + ret);
            }
        });
    }


    private void testHidden() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        Field field = null;
        try {
            field = wifiManager.getClass().getDeclaredField("WIFI_SCAN_AVAILABLE");
            Log.d("ThirdActivity", (String) field.get(wifiManager));
            toast("success: " + (String) field.get(wifiManager));
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            toast("error: " + e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            toast("error1: " + e);
        }

    }

    private void testHidden1() {
        try {

            Class<?> activityClass = Class.forName("dalvik.system.VMRuntime");
            Method field = activityClass.getDeclaredMethod("setHiddenApiExemptions", String[].class);
            field.setAccessible(true);

            Log.i(TAG, "call success!!");
        } catch (Throwable e) {
            Log.e(TAG, "error:", e);
            toast("error: " + e);
        }
    }

    private void toast(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Log.i(TAG, msg);
    }

    private void test() throws Throwable {
        Class<?> activityThread = Class.forName("android.app.ActivityThread");
        Class<?> hclass = Class.forName("android.app.ActivityThread$H");
        Field[] declaredFields = hclass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            Log.i(TAG, "declareField: " + declaredField);
        }
    }

    // 直接在黑名单中找的接口
    public void testBlackList() {
        try {
            Class<?> activityClass = Class.forName("android.net.util.IpUtils");
            Method field = activityClass.getDeclaredMethod("ipChecksum", ByteBuffer.class, int.class);
            field.setAccessible(true);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

}
