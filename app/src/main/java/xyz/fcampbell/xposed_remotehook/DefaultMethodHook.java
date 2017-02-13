package xyz.fcampbell.xposed_remotehook;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Created by francois on 2017-02-13.
 */

public class DefaultMethodHook extends XC_MethodHook {
    private static final String TAG = "DefaultMethodHook";

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG, "Default hooking method: " + param.method.toString());
    }
}
