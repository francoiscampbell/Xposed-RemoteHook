package xyz.fcampbell.xposed_remotehook;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.BaseDexClassLoader;

/**
 * Created by francois on 2017-02-16.
 */
class InputStreamDexClassLoader {
    private BaseDexClassLoader dexClassLoader;

    InputStreamDexClassLoader(InputStream dexFile, String fileName, Context context, ClassLoader parent) throws IOException {
        File savedDexFile = saveFile(dexFile, context.getCacheDir(), fileName);
        dexClassLoader = new BaseDexClassLoader(savedDexFile.getAbsolutePath(), context.getCodeCacheDir(), null, parent);
    }

    <T> Class<T> loadClass(String name) throws ClassNotFoundException {
        //noinspection unchecked
        return (Class<T>) dexClassLoader.loadClass(name);
    }

    private File saveFile(InputStream dexFile, File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            int read;
            byte[] bytes = new byte[1024];
            while ((read = dexFile.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }
        return file;
    }
}
