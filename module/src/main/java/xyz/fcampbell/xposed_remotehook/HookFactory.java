package xyz.fcampbell.xposed_remotehook;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.BaseDexClassLoader;
import de.robv.android.xposed.XC_MethodHook;

class HookFactory {
    private Context context;

    HookFactory(Context context) {
        this.context = context;
    }

    XC_MethodHook makeHookImpl(Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        String hookImplName = intent.getStringExtra(RemoteHook.KEY_HOOK_IMPL_CLASS_NAME);
        try {
            ClassLoader classLoader = makeClassLoader(intent, hookImplName);
            return (XC_MethodHook) classLoader.loadClass(hookImplName).newInstance();
        } catch (IOException e) {
            return null;
        }
    }

    Method makeMethod(Intent intent) {
        if (intent.hasExtra(RemoteHook.KEY_METHOD)) {
            return intent.getParcelableExtra(RemoteHook.KEY_METHOD); //parceled Method object
        } else if (intent.hasExtra(RemoteHook.KEY_CLASS_NAME)) { //class name as string
            Bundle extras = intent.getExtras();

            String className = extras.getString(RemoteHook.KEY_CLASS_NAME); //mandatory
            String methodName = extras.getString(RemoteHook.KEY_METHOD_NAME, ""); //optional, no method name == constructor
            String[] paramTypes = extras.containsKey(RemoteHook.KEY_PARAM_TYPES) ? extras.getStringArray(RemoteHook.KEY_PARAM_TYPES) : new String[0];// optional

            return Method.create(className, methodName, paramTypes);
        } else {
            throw new IllegalArgumentException("Intent extras do not contain a reference to a method to hook");
        }
    }

    private ClassLoader makeClassLoader(Intent intent, String hookImplName) throws IOException {
        String dexFilePath;

        if (intent.hasExtra(RemoteHook.KEY_HOOK_IMPL_RES_ID)) {
            String sourcePackageName = intent.getStringExtra(RemoteHook.KEY_SOURCE_PACKAGE);
            int classesDexId = intent.getIntExtra(RemoteHook.KEY_HOOK_IMPL_RES_ID, 0);

            try {
                Resources sourceResources = context.getPackageManager().getResourcesForApplication(sourcePackageName);
                InputStream dexFile = sourceResources.openRawResource(classesDexId);
                File savedDexFile = saveFile(dexFile, context.getCacheDir(), hookImplName + ".classes.dex");
                dexFilePath = savedDexFile.getAbsolutePath();
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Intent extras do not specify a valid source package", e);
            }
        } else if (intent.hasExtra(RemoteHook.KEY_HOOK_IMPL_DEX_FILE_BYTES)) {
            byte[] hookDexFileBytes = intent.getByteArrayExtra(RemoteHook.KEY_HOOK_IMPL_DEX_FILE_BYTES);
            InputStream dexFile = new ByteArrayInputStream(hookDexFileBytes);
            File savedDexFile = saveFile(dexFile, context.getCacheDir(), hookImplName + ".classes.dex");
            dexFilePath = savedDexFile.getAbsolutePath();
        } else if (intent.hasExtra(RemoteHook.KEY_HOOK_IMPL_DEX_FILE_PATH)) {
            dexFilePath = intent.getStringExtra(RemoteHook.KEY_HOOK_IMPL_DEX_FILE_PATH);
        } else {
            throw new IllegalArgumentException("Intent extras do not contain a reference to a dex file");
        }

        return new BaseDexClassLoader(dexFilePath, context.getCodeCacheDir(), null, ClassLoader.getSystemClassLoader());
    }

    private File saveFile(InputStream dexFile, File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = dexFile.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
        return file;
    }
}