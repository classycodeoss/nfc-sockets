package com.classycode.nfcsockets.messages;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public abstract class SocketResponse extends SocketMessage {

    protected int inReplyTo;

    public int getInReplyTo() {
        return inReplyTo;
    }

}
