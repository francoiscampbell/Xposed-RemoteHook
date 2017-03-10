package xyz.fcampbell.xposed_remotehook;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by francois on 2017-02-16.
 */

class MethodHook {
    private ClassLoader classLoader;

    private Map<Method, XC_MethodHook.Unhook> hookedMethods = new HashMap<>();

    MethodHook(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    void hook(Method method, XC_MethodHook hookImpl) {
        Object[] paramTypesAndHookImpl = append(method.paramTypes().toArray(), hookImpl);
        XposedBridge.log(Arrays.deepToString(method.paramTypes().toArray()));
        XposedBridge.log(hookImpl.toString());
        XposedBridge.log(Arrays.deepToString(paramTypesAndHookImpl));

        XC_MethodHook.Unhook unhook;
        if (method.isConstructor()) {
            unhook = XposedHelpers.findAndHookConstructor(
                    method.className(),
                    classLoader,
                    paramTypesAndHookImpl);
        } else {
            unhook = XposedHelpers.findAndHookMethod(
                    method.className(),
                    classLoader,
                    method.methodName(),
                    paramTypesAndHookImpl);
        }

        hookedMethods.put(method, unhook);
    }

    void unhook(Method method) {
        XC_MethodHook.Unhook unhook = hookedMethods.remove(method);
        if (unhook == null) return;

        unhook.unhook();
    }

    private Object[] append(Object[] array, Object val) {
        Object[] newArray = new Object[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = val;
        return newArray;
    }

}
