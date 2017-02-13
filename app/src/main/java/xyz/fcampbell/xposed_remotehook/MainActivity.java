package xyz.fcampbell.xposed_remotehook;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.RawRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.hookTestMethod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Sending broadcast using context: " + getApplication());
                try {
                    Intent hookIntent = new Intent(BuildConfig.APPLICATION_ID + ".hookMethod")
                            .putExtra("className", MainActivity.class.getCanonicalName())
                            .putExtra("methodName", "testMethod")
                            .putExtra("hookImpl", "xyz.fcampbell.xposed_remotehook.hooks.DefaultMethodHook")
                            .putExtra("hookDexFile", dexToBytes(R.raw.classes))
                            .setPackage(BuildConfig.APPLICATION_ID);

//                    File dexDir = getCodeCacheDir();
//                    File dexFile = new File(getCacheDir(), "classes.dex");
//                    DexClassLoader dcl = new DexClassLoader(
//                            dexFile.getAbsolutePath(),
//                            dexDir.getAbsolutePath(),
//                            null,
//                            ClassLoader.getSystemClassLoader());
//                    Class<XC_MethodHook> hookImplClass = (Class<XC_MethodHook>) dcl.loadClass("xyz.fcampbell.xposed_remotehook.hooks.DefaultMethodHook");
//                    XC_MethodHook mh = hookImplClass.newInstance();

                    sendBroadcast(hookIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.callTestMethod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testMethod();
            }
        });
    }

    public void testMethod() {
        Log.d(TAG, "Test method to be hooked");
    }

    public byte[] dexToBytes(@RawRes int dexFile) throws IOException {
        InputStream classInputStream = getResources().openRawResource(dexFile);
        try (BufferedInputStream bis = new BufferedInputStream(classInputStream);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(bis.available())) {
            byte[] buffer = new byte[0xFFFF];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1)
                baos.write(buffer, 0, bytesRead);

            return baos.toByteArray();
        }
    }
}
