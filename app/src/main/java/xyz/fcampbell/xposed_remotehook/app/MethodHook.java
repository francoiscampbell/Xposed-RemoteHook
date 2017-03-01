package xyz.fcampbell.xposed_remotehook.app;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by francois on 2017-02-16.
 */

public class MethodHook {
    private ClassLoader classLoader;

    private Map<Method, XC_MethodHook.Unhook> hookedMethods = new HashMap<>();

    public MethodHook(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void hook(Method method, XC_MethodHook hookImpl) {
        if (hookedMethods.containsKey(method)) return;

        Object[] paramTypesAndHookImpl = append(method.paramTypes().toArray(), hookImpl);
        XposedBridge.log(Arrays.deepToString(method.paramTypes().toArray()));
        XposedBridge.log(hookImpl.toString());
        XposedBridge.log(Arrays.deepToString(paramTypesAndHookImpl));

        XC_MethodHook.Unhook unhook;
        if (method.isConstructor()) {
            unhook = XposedHelpers.findAndHookConstructor(method.className(), classLoader, paramTypesAndHookImpl);
        } else {
            unhook = XposedHelpers.findAndHookMethod(method.className(), classLoader, method.methodName(), paramTypesAndHookImpl);
        }

        hookedMethods.put(method, unhook);
    }

    public void unhook(Method method) {
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

    @AutoValue
    public static abstract class Method implements Parcelable, Comparable<Method> {
        public static Method create(String packageName, String className, String methodName, List<String> paramTypes) {
            return new AutoValue_MethodHook_Method(packageName, className, methodName, paramTypes);
        }

        public abstract String packageName();

        public abstract String className();

        public abstract String methodName();

        public abstract List<String> paramTypes();

        public boolean hasParams() {
            return paramTypes().isEmpty();
        }

        public boolean isConstructor() {
            return methodName().isEmpty();
        }

        @Override
        public int compareTo(@NonNull Method o) {
            return toString().compareTo(o.toString());
        }
    }
}
