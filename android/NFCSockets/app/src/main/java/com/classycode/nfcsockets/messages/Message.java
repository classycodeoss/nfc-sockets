package com.classycode.nfcsockets.messages;

import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public abstract class Message {

    // message types
    protected static final byte MSG_NFC = 0;
    protected static final byte MSG_SOCKET = 1;

    // message sub types
    protected static final byte MSG_NFC_LINK_KEEP_ALIVE = 0;
    protected static final byte MSG_NFC_LINK_TERMINATE = 1;

    protected static final byte MSG_SOCKET_CONNECT = 0;
    protected static final byte MSG_SOCKET_SEND = 1;
    protected static final byte MSG_SOCKET_RECV = 2;
    protected static final byte MSG_SOCKET_CLOSE = 3;

    protected static final byte MSG_SOCKET_CONNECT_RESPONSE = 0;
    protected static final byte MSG_SOCKET_SEND_RESPONSE = 1;
    protected static final byte MSG_SOCKET_RECV_RESPONSE = 2;
    protected static final byte MSG_SOCKET_CLOSE_RESPONSE = 3;

    public static Message parseMessage(byte[] apdu) throws IOException {
        if (apdu.length < 2) {
            throw new IllegalArgumentException("APDU length too small");
        }

        final Message message;
        final byte type = apdu[0];
        final byte subtype = apdu[1];
        if (type == MSG_NFC) { // Ready
            switch (subtype) {
                case MSG_NFC_LINK_KEEP_ALIVE:
                    message = new KeepAliveMessage();
                    break;
                case MSG_NFC_LINK_TERMINATE:
                    message = new LinkTerminateMessage();
                    break;
                default:
                    throw new IllegalArgumentException("Illegal message sub type: " + subtype);
            }
            message.fromApdu(apdu);
            return message;
        } else if (type == MSG_SOCKET) { // Socket response
            switch (subtype) {
                case MSG_SOCKET_CONNECT_RESPONSE:
                    message = new ConnectResponse();
                    break;
                case MSG_SOCKET_SEND_RESPONSE:
                    message = new SendResponse();
                    break;
                case MSG_SOCKET_RECV_RESPONSE:
                    message = new RecvResponse();
                    break;
                case MSG_SOCKET_CLOSE_RESPONSE:
                    message = new CloseResponse();
                    break;
                default:
                    throw new IllegalArgumentException("Illegal message sub type: " + subtype);
            }
            message.fromApdu(apdu);
            return message;
        } else {
            throw new IllegalArgumentException("Illegal message type: " + type);
        }
    }

    public void fromApdu(byte[] apdu) throws IOException {
        throw new IllegalStateException("Not implemented");
    }

    public byte[] toApdu() {
        throw new IllegalStateException("Not implemented");
    }

}
