package xyz.fcampbell.xposed_remotehook;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.hookTestMethod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    RemoteHook.hookMethod(MainActivity.this,
                            BuildConfig.APPLICATION_ID,
                            MainActivity.class.getCanonicalName(),
                            "testMethod",
                            new String[]{int.class.getCanonicalName()},
                            "xyz.fcampbell.xposed_remotehook.hooks.IncrementArg",
                            R.raw.classes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        findViewById(R.id.unhookTestMethod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteHook.unhookMethod(MainActivity.this,
                        BuildConfig.APPLICATION_ID,
                        MainActivity.class.getCanonicalName(),
                        "testMethod",
                        new String[]{int.class.getCanonicalName()});
            }
        });

        findViewById(R.id.callTestMethod).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testMethod();
                testMethod(1);
            }
        });
    }

    public void testMethod() {
        Log.d(TAG, "Test method to be hooked");
    }

    public void testMethod(int num) {
        Log.d(TAG, "Test method taking an int: " + num);
    }
}
