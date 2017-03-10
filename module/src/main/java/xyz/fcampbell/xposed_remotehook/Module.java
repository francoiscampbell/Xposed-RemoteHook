package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main Xposed module
 */
public class Module implements IXposedHookLoadPackage {
    private static final String TAG = "Module";

    private static ClassLoader packageClassLoader;

    public static ClassLoader getPackageClassLoader() {
        return packageClassLoader;
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        packageClassLoader = lpparam.classLoader;

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application currentApp = (Application) param.thisObject;
                currentApp.getPackageManager().setComponentEnabledSetting(
                        new ComponentName(currentApp, RemoteHookService.class),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);
            }
        });
    }
}
