package xyz.fcampbell.xposed_remotehook;

import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.auto.value.AutoValue;

import java.util.Arrays;
import java.util.List;

/**
 * Created by francois on 2017-03-09.
 */
@AutoValue
public abstract class Method implements Parcelable, Comparable<Method> {
    public static Method create(String className, String methodName, List<String> paramTypes) {
        return new AutoValue_Method(className, methodName, paramTypes);
    }

    public static Method create(String className, String methodName, String[] paramTypes) {
        return new AutoValue_Method(className, methodName, Arrays.asList(paramTypes));
    }

    public abstract String className();

    public abstract String methodName();

    public abstract List<String> paramTypes();

    public boolean hasParams() {
        return paramTypes().isEmpty();
    }

    public boolean isConstructor() {
        return methodName().isEmpty();
    }

    @Override
    public int compareTo(@NonNull Method o) {
        return toString().compareTo(o.toString());
    }
}
