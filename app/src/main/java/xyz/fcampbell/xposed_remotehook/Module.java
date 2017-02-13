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
                final Application currentApp = (Application) param.thisObject;
                XposedBridge.log(TAG + "/Hooked package: " + lpparam.packageName + ", application: " + currentApp);

                if (currentApp == null) return;

                currentApp.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String className = intent.getStringExtra("className");
                        String methodName = intent.getStringExtra("methodName");
                        String hookImplName = intent.getStringExtra("hookImpl");
                        byte[] hookDexFileBytes = intent.getByteArrayExtra("hookDexFile");

                        XposedBridge.log(TAG + "/Hooking method: " + className + "#" + methodName);

                        try {
                            File dexDir = currentApp.getCodeCacheDir();
                            File dexFile = saveFile(hookDexFileBytes, currentApp.getCacheDir(), "classes.dex");
                            DexClassLoader dcl = new DexClassLoader(
                                    dexFile.getAbsolutePath(),
                                    dexDir.getAbsolutePath(),
                                    null,
                                    ClassLoader.getSystemClassLoader());
                            Class<XC_MethodHook> hookImplClass = (Class<XC_MethodHook>) dcl.loadClass(hookImplName);
                            XC_MethodHook hookImpl = hookImplClass.newInstance();
                            XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName, hookImpl);
                        } catch (Exception e) {
                            XposedBridge.log(e);
                        }
                    }
                }, new IntentFilter(BuildConfig.APPLICATION_ID + ".hookMethod"));
            }
        });
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
}
