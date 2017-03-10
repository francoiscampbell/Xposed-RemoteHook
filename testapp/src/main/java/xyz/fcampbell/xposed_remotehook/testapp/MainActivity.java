package xyz.fcampbell.xposed_remotehook.testapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.RawRes;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import xyz.fcampbell.xposed_remotehook.IRemoteHook;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @BindView(R.id.hookedPackage)
    EditText hookedPackage;

    @BindView(R.id.hookedClassName)
    EditText hookedClassName;

    @BindView(R.id.hookedMethodName)
    EditText hookedMethodName;

    @BindView(R.id.hookedMethodParams)
    EditText hookedMethodParams;

    @BindView(R.id.hookImplClass)
    EditText hookImplClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
    }

    @OnClick(R.id.hook)
    void hook() {
        ComponentName componentName = new ComponentName(hookedPackage.getText().toString(), "xyz.fcampbell.xposed_remotehook.RemoteHookService");
        Intent intent = new Intent().setComponent(componentName);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    IRemoteHook remoteHook = IRemoteHook.Stub.asInterface(service);
                    String paramsListString = hookedMethodParams.getText().toString();
                    String[] paramsList = paramsListString.isEmpty() ? new String[0] : paramsListString.split(",");
                    remoteHook.hookMethod(
                            hookedClassName.getText().toString(),
                            hookedMethodName.getText().toString(),
                            paramsList,
                            hookImplClass.getText().toString(),
                            dexToBytes(MainActivity.this, R.raw.classes));
                } catch (IOException | RemoteException e) {
                    Log.e(TAG, "Exception when hooking method", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    @OnClick(R.id.unhook)
    void unhook() {
        bindService(new Intent().setPackage(hookedPackage.getText().toString()), new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    IRemoteHook remoteHook = IRemoteHook.Stub.asInterface(service);
                    String paramsListString = hookedMethodParams.getText().toString();
                    String[] paramsList = paramsListString.isEmpty() ? new String[0] : paramsListString.split(",");
                    remoteHook.unhookMethod(
                            hookedClassName.getText().toString(),
                            hookedMethodName.getText().toString(),
                            paramsList);
                } catch (RemoteException e) {
                    Log.e(TAG, "Exception when unhooking method", e);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    private static byte[] dexToBytes(Context context, @RawRes int dexFile) throws IOException {
        InputStream classInputStream = context.getResources().openRawResource(dexFile);
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
