package xyz.fcampbell.xposed_remotehook.classloader;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import dalvik.system.BaseDexClassLoader;

/**
 * Created by francois on 2017-02-16.
 */
public class ByteArrayDexClassLoader {
    private BaseDexClassLoader dexClassLoader;

    public ByteArrayDexClassLoader(byte[] dexFile, String fileName, Context context, ClassLoader parent) throws IOException {
        File savedDexFile = saveFile(dexFile, context.getCacheDir(), fileName);
        dexClassLoader = new BaseDexClassLoader(savedDexFile.getAbsolutePath(), context.getCodeCacheDir(), null, parent);
    }

    public <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        //noinspection unchecked
        return (Class<T>) dexClassLoader.loadClass(name);
    }

    private File saveFile(byte[] dexFileBytes, File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(dexFileBytes);
        }
        return file;
    }
}
