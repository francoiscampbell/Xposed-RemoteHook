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
    static final String SOURCE_PACKAGE = "sourcePackage";
    static final String HOOK_IMPL_RES_ID = "resId";
    static final String METHOD = "method";
    static final String HOOK_IMPL_CLASS_NAME = "hookImplClassName";
    static final String HOOK_IMPL_DEX_FILE = "hookImplDexFile";

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
                                  Method method,
                                  String hookImplClassName,
                                  @RawRes int hookImplDexFileRes) throws IOException {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(ACTION_HOOK)
                .putExtra(SOURCE_PACKAGE, context.getPackageName())
                .putExtra(METHOD, method)
                .putExtra(HOOK_IMPL_CLASS_NAME, hookImplClassName)
                .putExtra(HOOK_IMPL_RES_ID, hookImplDexFileRes)
                .setPackage(method.packageName()));
    }

    /**
     * TODO This method lives in the local module
     *
     * @param context
     * @param method
     * @return
     */
    public static void unhookMethod(Context context,
                                    Method method) {
        Log.d(TAG, "Sending broadcast using context: " + context.getApplicationContext());
        context.sendBroadcast(new Intent(ACTION_UNHOOK)
                .putExtra(METHOD, method)
                .setPackage(method.packageName()));
    }
}
