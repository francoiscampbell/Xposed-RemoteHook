package xyz.fcampbell.xposed_remotehook;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RemoteHookService extends Service {
    private RemoteHook remoteHook;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        if (remoteHook == null) {
            remoteHook = new RemoteHook(this);
        }
        return remoteHook;
    }
}
