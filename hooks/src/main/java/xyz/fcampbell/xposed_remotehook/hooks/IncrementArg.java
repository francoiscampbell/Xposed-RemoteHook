package xyz.fcampbell.xposed_remotehook.hooks;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;

/**
 * Created by francois on 2017-02-13.
 */

public class IncrementArg extends XC_MethodHook {
    private static final String TAG = "IncrementArg";

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        Log.d(TAG, "Incrementing arg of method: " + param.method.toString());
        int incrementedArg = ((int) param.args[0]) + 1;

        if (param.method instanceof Method) {
            param.setResult(((Method) param.method).invoke(param.thisObject, incrementedArg));
        } else if (param.method instanceof Constructor) {
            param.setResult(((Constructor) param.method).newInstance(incrementedArg));
        }
    }
}
