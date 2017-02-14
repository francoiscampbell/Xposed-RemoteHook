package xyz.fcampbell.xposed_remotehook;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dalvik.system.DexClassLoader;
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

    private Map<Set<String>, XC_MethodHook.Unhook> hookedMethods = new HashMap<>();

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {

    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {

    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Application currentApp = (Application) param.thisObject;
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        hookMethod(context, intent, lpparam.classLoader);
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

    private void hookMethod(Context context, Intent intent, ClassLoader classLoader) {
        if (!validateIntentExtras(intent)) {
            XposedBridge.log(TAG + "/Couldn't hook method, intent did't have the required extras");
            return;
        }

        String className = intent.getStringExtra(CLASS_NAME);
        String methodName = intent.getStringExtra(METHOD_NAME);
        String[] paramTypes = getParamTypes(intent);
        String hookImplName = intent.getStringExtra(HOOK_IMPL_CLASS_NAME);
        byte[] hookDexFileBytes = intent.getByteArrayExtra(HOOK_IMPL_DEX_FILE);

        XposedBridge.log(TAG + "/Hooking method: " + className + "#" + methodName);


        XC_MethodHook hookImpl = makeHookImpl(context, hookDexFileBytes, hookImplName);
        if (hookImpl == null) {
            XposedBridge.log(TAG + "/Couldn't hook method, " + hookImplName + " couldn't be loaded");
            return;
        }

        Object[] paramTypesAndCallback = new Object[paramTypes.length + 1];
        System.arraycopy(paramTypes, 0, paramTypesAndCallback, 0, paramTypes.length);
        paramTypesAndCallback[paramTypes.length] = hookImpl;
        XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(className, classLoader, methodName, paramTypesAndCallback);
        Set<String> hookParams = makeSet(className, methodName, paramTypes);
        hookedMethods.put(hookParams, unhook);
    }

    private void unhookMethod(Intent intent) {
        if (!validateIntentExtras(intent)) {
            XposedBridge.log(TAG + "/Couldn't unhook method, intent did't have the required extras");
            return;
        }

        String className = intent.getStringExtra(CLASS_NAME);
        String methodName = intent.getStringExtra(METHOD_NAME);
        String[] paramTypes = getParamTypes(intent);

        XposedBridge.log(TAG + "/Unhooking method: " + className + "#" + methodName);

        Set<String> hookParams = makeSet(className, methodName, paramTypes);
        XC_MethodHook.Unhook unhook = hookedMethods.remove(hookParams);
        if (unhook != null) unhook.unhook();
    }

    private XC_MethodHook makeHookImpl(Context context, byte[] hookDexFileBytes, String hookImplName) {
        try {
            File dexDir = context.getCodeCacheDir();
            File dexFile = saveFile(hookDexFileBytes, context.getCacheDir(), hookImplName + ".classes.dex");

            DexClassLoader dcl = new DexClassLoader(
                    dexFile.getAbsolutePath(),
                    dexDir.getAbsolutePath(),
                    null,
                    ClassLoader.getSystemClassLoader());
            Class<?> hookClass = dcl.loadClass(hookImplName);
            return (XC_MethodHook) hookClass.newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private File saveFile(byte[] dexFileBytes, File dir, String fileName) throws IOException {
        File dexFile = new File(dir, fileName);
        dexFile.createNewFile();
        try (BufferedOutputStream dexWriter = new BufferedOutputStream(new FileOutputStream(dexFile))) {
            dexWriter.write(dexFileBytes);
            dexWriter.close();
        }
        return dexFile;
    }

    private boolean validateIntentExtras(Intent intent) {
        return intent.hasExtra(CLASS_NAME)
                && intent.hasExtra(METHOD_NAME);
    }

    private boolean validateIntentExtrasForHooking(Intent intent) {
        return validateIntentExtras(intent)
                && intent.hasExtra(HOOK_IMPL_CLASS_NAME)
                && intent.hasExtra(HOOK_IMPL_DEX_FILE);
    }

    private boolean hookedMethodHasParams(Intent intent) {
        return intent.hasExtra(PARAM_TYPES);
    }

    private Set<String> makeSet(String className,
                                String methodName,
                                String[] paramTypes) {
        Set<String> hookParams = new HashSet<>();
        hookParams.add(className);
        hookParams.add(methodName);
        hookParams.addAll(Arrays.asList(paramTypes));
        return hookParams;
    }

    private String[] getParamTypes(Intent intent) {
        return hookedMethodHasParams(intent) ? intent.getStringArrayExtra(PARAM_TYPES) : new String[0];
    }
}
