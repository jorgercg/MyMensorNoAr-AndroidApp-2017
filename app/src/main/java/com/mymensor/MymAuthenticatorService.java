package com.mymensor;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MymAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        MymAccAuthenticator authenticator = new MymAccAuthenticator(this);
        return authenticator.getIBinder();
    }
}
