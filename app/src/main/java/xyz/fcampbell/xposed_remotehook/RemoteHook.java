package xyz.fcampbell.xposed_remotehook;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.RawRes;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by francois on 2017-02-14.
 */

public class RemoteHook {
    private static final String TAG = "RemoteHook";

    /**
     * TODO This method lives in the local module
     *
     * @param context
     * @param packageName
     * @param className
     * @param methodName
     * @param paramTypes
     * @param hookImplClassName
     * @param hookImplDexFileRes
     * @return
     */
    static void hookMethod(Context context,
                           String packageName,
                           String className,
                           String methodName,
                           String[] paramTypes,
                           String hookImplClassName,
                           @RawRes int hookImplDexFileRes) throws IOException {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(Module.ACTION_HOOK)
                .putExtra(Module.CLASS_NAME, className)
                .putExtra(Module.METHOD_NAME, methodName)
                .putExtra(Module.PARAM_TYPES, paramTypes)
                .putExtra(Module.HOOK_IMPL_CLASS_NAME, hookImplClassName)
                .putExtra(Module.HOOK_IMPL_DEX_FILE, dexToBytes(context, hookImplDexFileRes))
                .setPackage(packageName));
    }

    /**
     * TODO This method lives in the local module
     *
     * @param context
     * @param packageName
     * @param className
     * @param methodName
     * @param paramTypes
     * @return
     */
    static void unhookMethod(Context context,
                             String packageName,
                             String className,
                             String methodName,
                             String[] paramTypes) {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(Module.ACTION_UNHOOK)
                .putExtra(Module.CLASS_NAME, className)
                .putExtra(Module.METHOD_NAME, methodName)
                .putExtra(Module.PARAM_TYPES, paramTypes)
                .setPackage(packageName));
    }

    private static byte[] dexToBytes(Context context, @RawRes int dexFile) throws IOException {
        InputStream classInputStream = context.getResources().openRawResource(dexFile);
        try (BufferedInputStream bis = new BufferedInputStream(classInputStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(bis.available())) {
            byte[] buffer = new byte[0xFFFF];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1)
                baos.write(buffer, 0, bytesRead);

            return baos.toByteArray();
        }
    }
}
