package com.classycode.nfcsockets.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class SendResponse extends SocketResponse {

    private int res;

    public SendResponse() {
        res = -1;
    }

    @Override
    public void fromApdu(byte[] apdu) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(apdu));
        if (in.readByte() != MSG_SOCKET || in.readByte() != MSG_SOCKET_SEND_RESPONSE) {
            throw new IllegalArgumentException("Malformed APDU");
        }
        inReplyTo = in.readInt();
        res = in.readInt();
    }

    public int getRes() {
        return res;
    }

    public boolean isSuccess() {
        return res > 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SendResponse{");
        sb.append("inReplyTo=").append(inReplyTo);
        sb.append(", res=").append(res);
        sb.append('}');
        return sb.toString();
    }
}
