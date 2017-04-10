package com.classycode.nfcsockets.messages;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public abstract class SocketRequest extends SocketMessage {

    protected final int requestId;

    private static int nextRequestId = 0;

    private static synchronized int nextRequestId() {
        nextRequestId++;
        if (nextRequestId <= 0) { // handle signed int overflow
            nextRequestId = 1;
        }
        return nextRequestId;
    }

    SocketRequest() {
        requestId = nextRequestId();
    }

    public int getRequestId() {
        return requestId;
    }
}
