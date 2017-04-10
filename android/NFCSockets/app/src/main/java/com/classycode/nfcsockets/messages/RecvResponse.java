package com.classycode.nfcsockets.messages;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class RecvResponse extends SocketResponse {

    private int res;

    private byte[] data;

    public RecvResponse() {
        res = -1;
        data = null;
    }

    @Override
    public void fromApdu(byte[] apdu) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(apdu));
        if (in.readByte() != MSG_SOCKET || in.readByte() != MSG_SOCKET_RECV_RESPONSE) {
            throw new IllegalArgumentException("Malformed APDU");
        }
        inReplyTo = in.readInt();
        res = in.readInt();
        if (res > 0) {
            data = new byte[res];
            in.read(data);
        }
    }

    public int getRes() {
        return res;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isSuccess() {
        return res > 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RecvResponse{");
        sb.append("inReplyTo=").append(inReplyTo);
        sb.append(", res=").append(res);
        sb.append('}');
        return sb.toString();
    }
}
