package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Main Xposed module
 */
@SuppressWarnings("unused")
public class Module implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private static final String TAG = "Module";

    private HookFactory hookFactory;
    private MethodHook mh;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        mh = new MethodHook(lpparam.classLoader);

        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application currentApp = (Application) param.thisObject;
                hookFactory = new HookFactory(currentApp);
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        hookMethod(intent);
                    }
                }, new IntentFilter(RemoteHook.ACTION_HOOK));

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        unhookMethod(intent);
                    }
                }, new IntentFilter(RemoteHook.ACTION_UNHOOK));
            }
        });
    }

    private void hookMethod(Intent intent) {
        try {
            XC_MethodHook hookImpl = hookFactory.makeHookImpl(intent);
            Method m = hookFactory.makeMethod(intent);
            XposedBridge.log(TAG + "/Hooking method: " + m.className() + "#" + m.methodName());
            mh.hook(m, hookImpl);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void unhookMethod(Intent intent) {
        try {
            Method m = hookFactory.makeMethod(intent);
            XposedBridge.log(TAG + "/Unhooking method: " + m.className() + "#" + m.methodName() + m.paramTypes().toString());
            mh.unhook(m);
        } catch (IllegalArgumentException e) {
            XposedBridge.log(e);
        }
    }
}
