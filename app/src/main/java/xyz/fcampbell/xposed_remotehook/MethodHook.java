package xyz.fcampbell.xposed_remotehook;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
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

        XC_MethodHook.Unhook unhook;
        Object[] paramTypesAndHookImpl = append(method.getParamTypes(), hookImpl);
        if (method.isConstructor()) {
            unhook = XposedHelpers.findAndHookConstructor(method.getClassName(), classLoader, paramTypesAndHookImpl);
        } else {
            unhook = XposedHelpers.findAndHookMethod(method.getClassName(), classLoader, method.getMethodName(), paramTypesAndHookImpl);
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

    public static class Method {
        private String className;
        private String methodName;
        private String[] paramTypes;

        //method
        public Method(String className, String methodName, String[] paramTypes) {
            this.className = className != null ? className : "";
            this.methodName = methodName != null ? methodName : "";
            this.paramTypes = paramTypes != null ? paramTypes : new String[0];
        }

        //method with no params
        public Method(String className, String methodName) {
            this(className, methodName, new String[0]);
        }

        //constructor
        public Method(String className, String[] paramTypes) {
            this(className, "", paramTypes);
        }

        //default constructor
        public Method(String className) {
            this(className, "", new String[0]);
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public String[] getParamTypes() {
            return paramTypes;
        }

        public boolean hasParams() {
            return paramTypes.length > 0;
        }

        public boolean isConstructor() {
            return methodName.isEmpty();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Method method = (Method) o;

            if (!getClassName().equals(method.getClassName())) return false;
            if (!getMethodName().equals(method.getMethodName())) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(getParamTypes(), method.getParamTypes());

        }

        @Override
        public int hashCode() {
            int result = getClassName().hashCode();
            result = 31 * result + getMethodName().hashCode();
            result = 31 * result + Arrays.hashCode(getParamTypes());
            return result;
        }
    }
}
