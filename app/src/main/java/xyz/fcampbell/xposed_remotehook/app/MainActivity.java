package xyz.fcampbell.xposed_remotehook.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import xyz.fcampbell.xposed_remotehook.R;

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

    @BindView(R.id.hookedMethods)
    Spinner hookedMethodsSpinner;

    private Set<MethodHook.Method> hookedMethods = new TreeSet<>();
    private ArrayAdapter<MethodHook.Method> hookedMethodsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        hookedMethodsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hookedMethods.toArray(new MethodHook.Method[0]));
        hookedMethodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        hookedMethodsSpinner.setAdapter(hookedMethodsAdapter);
    }

    @OnClick(R.id.hook)
    void hook() {
        try {
            List<String> paramList = Arrays.asList(hookedMethodParams.getText().toString().split(","));
            if (paramList.get(0).isEmpty()) paramList = paramList.subList(1, paramList.size());

            MethodHook.Method m = MethodHook.Method.create(
                    hookedPackage.getText().toString(),
                    hookedClassName.getText().toString(),
                    hookedMethodName.getText().toString(),
                    paramList);
            Log.d(TAG, m.toString());
            RemoteHook.hookMethod(MainActivity.this, m, hookImplClass.getText().toString(), R.raw.classes);
            hookedMethods.add(m);
            hookedMethodsAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @OnClick(R.id.unhook)
    void unhook() {
        MethodHook.Method m = (MethodHook.Method) hookedMethodsSpinner.getSelectedItem();
        RemoteHook.unhookMethod(MainActivity.this, m);
        hookedMethods.remove(m);
    }
}
