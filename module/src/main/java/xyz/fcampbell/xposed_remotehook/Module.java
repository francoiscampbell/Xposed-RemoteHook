package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.Context;

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
        final RemoteHook[] remoteHook = new RemoteHook[1]; //to get around final variables in callbacks

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application currentApp = (Application) param.thisObject;
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);
            }
        });

        XposedHelpers.findAndHookMethod("android.app.ContextImpl", lpparam.classLoader, "getSystemService", String.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if ("test".equals(param.args[0])) {
                    XposedBridge.log(TAG + "/Retrieving RemoteHook in app: " + lpparam.packageName);

                    if (remoteHook[0] == null) {
                        remoteHook[0] = new RemoteHook((Context) param.thisObject);
                    }
                    param.setResult(IRemoteHook.Stub.asInterface(remoteHook[0]));
                }
            }
        });
    }
}
