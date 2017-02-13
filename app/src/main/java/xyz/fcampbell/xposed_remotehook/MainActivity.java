package xyz.fcampbell.xposed_remotehook;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

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
                Intent hookIntent = new Intent(BuildConfig.APPLICATION_ID + ".hookMethod")
                        .putExtra("className", MainActivity.class.getCanonicalName())
                        .putExtra("methodName", "testMethod")
                        .setPackage(BuildConfig.APPLICATION_ID);
                sendBroadcast(hookIntent);
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
}
