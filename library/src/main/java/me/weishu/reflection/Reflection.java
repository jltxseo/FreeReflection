package me.weishu.reflection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static android.os.Build.VERSION.PREVIEW_SDK_INT;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.P;

/**
 * @author weishu
 * @date 2018/6/7.
 */

public class Reflection {
    private static final String TAG = "Reflection";

    private static Object sVmRuntime;
    private static Method setHiddenApiExemptions;

    static {
        try {
            Method forName = Class.class.getDeclaredMethod("forName", String.class);
            // 公开API，无问题
            Method getDeclaredMethod = Class.class.getDeclaredMethod("getDeclaredMethod", String.class, Class[].class);
            // 系统类通过反射使用隐藏 API，检查直接通过 正确找到 Method 直接反射调用
            Class<?> vmRuntimeClass = (Class<?>) forName.invoke(null, "dalvik.system.VMRuntime");
            Method getRuntime = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "getRuntime", null);
            setHiddenApiExemptions = (Method) getDeclaredMethod.invoke(vmRuntimeClass, "setHiddenApiExemptions", new Class[]{String[].class});
            sVmRuntime = getRuntime.invoke(null);
        } catch (Throwable e) {
            Log.e(TAG, "reflect bootstrap failed:", e);
        }

        System.loadLibrary("free-reflection");
    }

    private static native int unsealNative(int targetSdkVersion);

    private static int UNKNOWN = -9999;

    private static final int ERROR_SET_APPLICATION_FAILED = -20;

    private static int unsealed = UNKNOWN;

    private static void closeAndroidPDialog(){
        try {
            Class aClass = Class.forName("android.content.pm.PackageParser$Package");
            Constructor declaredConstructor = aClass.getDeclaredConstructor(String.class);
            declaredConstructor.setAccessible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Class cls = Class.forName("android.app.ActivityThread");
            Method declaredMethod = cls.getDeclaredMethod("currentActivityThread");
            declaredMethod.setAccessible(true);
            Object activityThread = declaredMethod.invoke(null);
            Field mHiddenApiWarningShown = cls.getDeclaredField("mHiddenApiWarningShown");
            mHiddenApiWarningShown.setAccessible(true);
            mHiddenApiWarningShown.setBoolean(activityThread, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int unseal(Context context) {
        if (SDK_INT < 28) {
            // Below Android P, ignore targetSdkVersion < 28(p) 则这时候是支持私有api的反射调用，只不过弹出提示
            // Detected problems with API compatibility对话框，这里面也去掉这对话框
            closeAndroidPDialog();
            return 0;
        }

        // try exempt API first.
        if (exemptAll()) {
            return 0;
        }

        if (context == null) {
            return -10;
        }

        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int targetSdkVersion = applicationInfo.targetSdkVersion;

        synchronized (Reflection.class) {
            if (unsealed != UNKNOWN) {
                return unsealed;
            }

            unsealed = unsealNative(targetSdkVersion);
            if (unsealed < 0) {
                return unsealed;
            }

            if ((SDK_INT == P && PREVIEW_SDK_INT > 0) || SDK_INT > P) {
                return unsealed;
            }

            // Android P, we need to sync the flags with ApplicationInfo
            // We needn't to this on Android Q.
            try {
                @SuppressLint("PrivateApi") Method setHiddenApiEnforcementPolicy = ApplicationInfo.class
                        .getDeclaredMethod("setHiddenApiEnforcementPolicy", int.class);
                setHiddenApiEnforcementPolicy.invoke(applicationInfo, 0);
            } catch (Throwable e) {
                e.printStackTrace();
                unsealed = ERROR_SET_APPLICATION_FAILED;
            }
        }

        return unsealed;
    }

    /**
     * make the method exempted from hidden API check.
     *
     * @param method the method signature prefix.
     * @return true if success.
     */
    public static boolean exempt(String method) {
        return exempt(new String[]{method});
    }

    /**
     * make specific methods exempted from hidden API check.
     *
     * @param methods the method signature prefix, such as "Ldalvik/system", "Landroid" or even "L"
     * @return true if success
     */
    public static boolean exempt(String... methods) {
        if (sVmRuntime == null || setHiddenApiExemptions == null) {
            return false;
        }

        try {
            setHiddenApiExemptions.invoke(sVmRuntime, new Object[]{methods});
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Make all hidden API exempted.
     *
     * @return true if success.
     */
    public static boolean exemptAll() {
        return exempt(new String[]{"L"});
    }
}
