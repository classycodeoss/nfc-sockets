package com.classycode.nfcsockets;

import org.greenrobot.eventbus.EventBus;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class SocketEventBus {

    public static final EventBus instance;

    static {
        instance = EventBus.builder().build();
    }

}
