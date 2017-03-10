// IRemoteHook.aidl
package xyz.fcampbell.xposed_remotehook;

// Declare any non-default types here with import statements

interface IRemoteHook {
    void hookMethod(String className,
                        String methodName,
                        in String[] parameterTypes,
                        String callbackClassName,
                        in byte[] dexFile);

    void hookConstructor(String className,
                        in String[] parameterTypes,
                        String callbackClassName,
                        in byte[] dexFile);

    void unhookMethod(String className, String methodName, in String[] parameterTypes);

    void unhookConstructor(String className, in String[] parameterTypes);
}
