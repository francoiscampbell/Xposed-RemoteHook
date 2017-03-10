package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import java.io.IOException;
import java.io.InputStream;

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
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        hookMethod(context, intent);
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

    private void hookMethod(Context context, Intent intent) {
        try {
            XC_MethodHook hookImpl = makeHookImpl(context, intent);
            Method m = getMethod(intent);
            XposedBridge.log(TAG + "/Hooking method: " + m.className() + "#" + m.methodName());
            mh.hook(m, hookImpl);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void unhookMethod(Intent intent) {
        try {
            Method m = getMethod(intent);
            XposedBridge.log(TAG + "/Unhooking method: " + m.className() + "#" + m.methodName() + m.paramTypes().toString());
            mh.unhook(m);
        } catch (IllegalArgumentException e) {
            XposedBridge.log(e);
        }
    }

    private XC_MethodHook makeHookImpl(Context context, Intent intent) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException, PackageManager.NameNotFoundException {
        if (!extrasContainDexFile(intent)) {
            throw new IllegalArgumentException("Intent extras do not contain " + RemoteHook.HOOK_IMPL_DEX_FILE + " and/or " + RemoteHook.HOOK_IMPL_CLASS_NAME);
        }

        String sourcePackageName = intent.getStringExtra(RemoteHook.SOURCE_PACKAGE);
        int classesDexId = intent.getIntExtra(RemoteHook.HOOK_IMPL_RES_ID, 0);

        Resources sourceResources = context.getPackageManager().getResourcesForApplication(sourcePackageName);
        InputStream dexFile = sourceResources.openRawResource(classesDexId);

        String hookImplName = intent.getStringExtra(RemoteHook.HOOK_IMPL_CLASS_NAME);
        InputStreamDexClassLoader classLoader = new InputStreamDexClassLoader(
                dexFile,
                hookImplName + ".classes.dex",
                context,
                ClassLoader.getSystemClassLoader());
        return classLoader.<XC_MethodHook>loadClass(hookImplName).newInstance();
    }

    private boolean extrasContainDexFile(Intent intent) {
        return intent.hasExtra(RemoteHook.SOURCE_PACKAGE)
                && intent.hasExtra(RemoteHook.HOOK_IMPL_RES_ID);
    }

    private Method getMethod(Intent intent) {
        return intent.getParcelableExtra(RemoteHook.METHOD);
    }
}
