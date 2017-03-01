package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xyz.fcampbell.xposed_remotehook.app.MethodHook;
import xyz.fcampbell.xposed_remotehook.classloader.ByteArrayDexClassLoader;

/**
 * Main Xposed module
 */
public class Module implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {
    private static final String TAG = "Module";

    public static final String ACTION_HOOK = BuildConfig.APPLICATION_ID + ".hook";
    public static final String ACTION_UNHOOK = BuildConfig.APPLICATION_ID + ".unhook";

    public static final String METHOD = "method";
    public static final String HOOK_IMPL_CLASS_NAME = "hookImplClassName";
    public static final String HOOK_IMPL_DEX_FILE = "hookImplDexFile";

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
            MethodHook.Method m = getMethod(intent);
            XposedBridge.log(TAG + "/Hooking method: " + m.className() + "#" + m.methodName());
            mh.hook(m, hookImpl);
        } catch (Exception e) {
            XposedBridge.log(e);
        }
    }

    private void unhookMethod(Intent intent) {
        try {
            MethodHook.Method m = getMethod(intent);
            XposedBridge.log(TAG + "/Unhooking method: " + m.className() + "#" + m.methodName() + m.paramTypes().toString());
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

    private MethodHook.Method getMethod(Intent intent) {
        return intent.getParcelableExtra(Module.METHOD);
    }
}
