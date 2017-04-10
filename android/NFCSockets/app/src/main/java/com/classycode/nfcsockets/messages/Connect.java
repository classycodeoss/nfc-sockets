package com.classycode.nfcsockets.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class Connect extends SocketRequest {

    private final String host;
    private final int port;

    public Connect(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public byte[] toApdu() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(MSG_SOCKET);
            out.writeByte(MSG_SOCKET_CONNECT);
            out.writeInt(requestId);
            out.writeShort(port);
            out.write(host.getBytes("ASCII")); // TODO
            return buf.toByteArray();
        } catch (IOException ex) { // should never happen
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Connect{");
        sb.append("requestId=").append(requestId);
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append('}');
        return sb.toString();
    }
}
