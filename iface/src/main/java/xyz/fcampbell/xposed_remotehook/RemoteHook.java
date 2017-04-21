package xyz.fcampbell.xposed_remotehook;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.RawRes;
import android.util.Log;

import java.io.IOException;

import xyz.fcampbell.xposed_remotehook.iface.BuildConfig;

/**
 * Created by francois on 2017-02-14.
 */

public class RemoteHook {
    private static final String TAG = "RemoteHook";

    static final String ACTION_HOOK = BuildConfig.APPLICATION_ID + ".hook";
    static final String ACTION_UNHOOK = BuildConfig.APPLICATION_ID + ".unhook";

    static final String KEY_METHOD = "method";

    static final String KEY_CLASS_NAME = "className";
    static final String KEY_METHOD_NAME = "methodName";
    static final String KEY_PARAM_TYPES = "paramTypes";

    static final String KEY_SOURCE_PACKAGE = "sourcePackage";
    static final String KEY_HOOK_IMPL_RES_ID = "hookImplResId";

    static final String KEY_HOOK_IMPL_CLASS_NAME = "hookImplClassName";
    static final String KEY_HOOK_IMPL_DEX_FILE_BYTES = "hookImplDexFile";
    static final String KEY_HOOK_IMPL_DEX_FILE_PATH = "hookImplDexFilePath";

    /**
     * TODO This method lives in the local module
     *
     * @param context
     * @param method
     * @param hookImplClassName
     * @param hookImplDexFileRes
     * @return
     */
    public static void hookMethod(Context context,
                                  String packageName,
                                  Method method,
                                  String hookImplClassName,
                                  @RawRes int hookImplDexFileRes) throws IOException {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(ACTION_HOOK)
                .putExtra(KEY_SOURCE_PACKAGE, context.getPackageName())
                .putExtra(KEY_METHOD, method)
                .putExtra(KEY_HOOK_IMPL_CLASS_NAME, hookImplClassName)
                .putExtra(KEY_HOOK_IMPL_RES_ID, hookImplDexFileRes)
                .setPackage(packageName));
    }

    /**
     * TODO This method lives in the local module
     *
     * @param context
     * @param method
     * @return
     */
    public static void unhookMethod(Context context,
                                    String packageName,
                                    Method method) {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(ACTION_UNHOOK)
                .putExtra(KEY_METHOD, method)
                .setPackage(packageName));
    }
}
