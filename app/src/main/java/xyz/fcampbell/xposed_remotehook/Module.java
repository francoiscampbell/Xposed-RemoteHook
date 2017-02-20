package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import java.io.IOException;

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
public class Module implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private static final String TAG = "Module";

    static final String ACTION_HOOK = BuildConfig.APPLICATION_ID + ".hook";
    static final String ACTION_UNHOOK = BuildConfig.APPLICATION_ID + ".unhook";

    static final String CLASS_NAME = "className";
    static final String METHOD_NAME = "methodName";
    static final String PARAM_TYPES = "paramTypes";
    static final String HOOK_IMPL_CLASS_NAME = "hookImplClassName";
    static final String HOOK_IMPL_DEX_FILE = "hookImplDexFile";

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
                }, new IntentFilter(ACTION_HOOK));

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        unhookMethod(intent);
                    }
                }, new IntentFilter(ACTION_UNHOOK));
            }
        });
    }

    private void hookMethod(Context context, Intent intent) {
        try {
            XC_MethodHook hookImpl = makeHookImpl(context, intent);
            MethodHook.Method m = makeMethod(intent);
            XposedBridge.log(TAG + "/Hooking method: " + m.getClassName() + "#" + m.getMethodName());
            mh.hook(m, hookImpl);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void unhookMethod(Intent intent) {
        try {
            MethodHook.Method m = makeMethod(intent);
            XposedBridge.log(TAG + "/Unhooking method: " + m.getClassName() + "#" + m.getMethodName());
            mh.unhook(m);
        } catch (IllegalArgumentException e) {
            XposedBridge.log(e);
        }
    }

    private XC_MethodHook makeHookImpl(Context context, Intent intent) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        if (!extrasContainDexFile(intent)) {
            throw new IllegalArgumentException("Intent extras do not contain " + HOOK_IMPL_DEX_FILE + " and/or " + HOOK_IMPL_CLASS_NAME);
        }

        String hookImplName = intent.getStringExtra(HOOK_IMPL_CLASS_NAME);
        byte[] hookDexFileBytes = intent.getByteArrayExtra(HOOK_IMPL_DEX_FILE);
        ByteArrayDexClassLoader classLoader = new ByteArrayDexClassLoader(
                hookDexFileBytes,
                hookImplName + ".classes.dex",
                context,
                ClassLoader.getSystemClassLoader());
        return classLoader.<XC_MethodHook>loadClass(hookImplName).newInstance();
    }

    private boolean extrasContainDexFile(Intent intent) {
        return intent.hasExtra(HOOK_IMPL_CLASS_NAME)
                && intent.hasExtra(HOOK_IMPL_DEX_FILE);
    }

    private MethodHook.Method makeMethod(Intent intent) {
        Bundle extras = intent.getExtras();

        String className = extras.getString(CLASS_NAME);
        if (className == null) {
            throw new IllegalArgumentException("Intent extras did not contain " + CLASS_NAME);
        }

        String methodName = extras.getString(METHOD_NAME, "");
        String[] paramTypes = extras.containsKey(PARAM_TYPES) ? extras.getStringArray(PARAM_TYPES) : new String[0];

        return new MethodHook.Method(className, methodName, paramTypes);
    }
}
