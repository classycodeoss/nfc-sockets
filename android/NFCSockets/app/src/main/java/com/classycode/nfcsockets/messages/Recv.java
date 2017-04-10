package com.classycode.nfcsockets.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class Recv extends SocketRequest {

    private final int fd;
    private final int len;

    public Recv(int fd, int len) {
        this.fd = fd;
        this.len = len;
    }

    @Override
    public byte[] toApdu() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(MSG_SOCKET);
            out.writeByte(MSG_SOCKET_RECV);
            out.writeInt(requestId);
            out.writeInt(fd);
            out.writeInt(len);
            return buf.toByteArray();
        } catch (IOException ex) { // should never happen
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Recv{");
        sb.append("requestId=").append(requestId);
        sb.append(", fd=").append(fd);
        sb.append(", len=").append(len);
        sb.append('}');
        return sb.toString();
    }
}
