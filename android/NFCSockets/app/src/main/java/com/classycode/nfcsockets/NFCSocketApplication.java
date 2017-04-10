package com.classycode.nfcsockets;

import android.app.Application;

import iaik.security.jsse.provider.IAIKJSSEProvider;
import iaik.security.jsse.utils.Debug;
import iaik.security.provider.IAIK;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCSocketApplication extends Application {

    @Override
    public void onCreate() {
        Debug.getInstance().setDebugMode(Debug.DEBUG_ENABLED);
        IAIKJSSEProvider.addAsProvider();
        IAIK.addAsProvider();

        super.onCreate();
    }
}
