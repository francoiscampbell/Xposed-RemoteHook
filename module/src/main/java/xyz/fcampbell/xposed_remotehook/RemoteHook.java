package xyz.fcampbell.xposed_remotehook;

import android.content.Context;
import android.os.RemoteException;

import java.io.IOException;
import java.lang.reflect.Member;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

/**
 * Created by francois on 2017-03-09.
 */
class RemoteHook extends IRemoteHook.Stub {
    private Context context;
    private Map<Member, XC_MethodHook.Unhook> hookedMethods = new HashMap<>();

    public RemoteHook(Context context) {
        this.context = context;
    }

    @Override
    public void hookMethod(String className, String methodName, String[] parameterTypes, String callbackClassName, byte[] dexFile) throws RemoteException {
        try {
            XC_MethodHook callback = getHookImpl(callbackClassName, dexFile);
            Object[] parameterTypesAndCallback = append(parameterTypes, callback);
            XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookMethod(
                    className,
                    Module.getPackageClassLoader(),
                    methodName,
                    parameterTypesAndCallback);
            hookedMethods.put(unhook.getHookedMethod(), unhook);
        } catch (Exception e) {
            RemoteException remoteException = new RemoteException();
            remoteException.addSuppressed(e);
            throw remoteException;
        }
    }

    @Override
    public void hookConstructor(String className, String[] parameterTypes, String callbackClassName, byte[] dexFile) throws RemoteException {
        try {
            XC_MethodHook callback = getHookImpl(callbackClassName, dexFile);
            Object[] parameterTypesAndCallback = append(parameterTypes, callback);
            XC_MethodHook.Unhook unhook = XposedHelpers.findAndHookConstructor(
                    className,
                    Module.getPackageClassLoader(),
                    parameterTypesAndCallback);
            hookedMethods.put(unhook.getHookedMethod(), unhook);
        } catch (Exception e) {
            RemoteException remoteException = new RemoteException();
            remoteException.addSuppressed(e);
            throw remoteException;
        }
    }

    @Override
    public void unhookMethod(String className, String methodName, String[] parameterTypes) throws RemoteException {
        unhookMember(XposedHelpers.findMethodExact(className, Module.getPackageClassLoader(), methodName, (Object[]) parameterTypes));
    }

    @Override
    public void unhookConstructor(String className, String[] parameterTypes) throws RemoteException {
        unhookMember(XposedHelpers.findConstructorExact(className, Module.getPackageClassLoader(), (Object[]) parameterTypes));
    }

    private void unhookMember(Member m) {
        XC_MethodHook.Unhook unhook = hookedMethods.remove(m);
        if (unhook != null) {
            unhook.unhook();
        }
    }

    private XC_MethodHook getHookImpl(String hookClassName, byte[] dexFile) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ByteArrayDexClassLoader classLoader = new ByteArrayDexClassLoader(
                dexFile,
                hookClassName + ".classes.dex",
                context,
                ClassLoader.getSystemClassLoader());
        return classLoader.<XC_MethodHook>loadClass(hookClassName).newInstance();
    }

    private Object[] append(Object[] array, Object val) {
        Object[] newArray = new Object[array.length + 1];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = val;
        return newArray;
    }
}
