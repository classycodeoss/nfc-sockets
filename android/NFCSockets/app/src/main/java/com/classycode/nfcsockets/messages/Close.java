package com.classycode.nfcsockets.messages;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class Close extends SocketRequest {

    private final int fd;

    public Close(int fd) {
        this.fd = fd;
    }

    @Override
    public byte[] toApdu() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(MSG_SOCKET);
            out.writeByte(MSG_SOCKET_CLOSE);
            out.writeInt(requestId);
            out.writeInt(fd);
            return buf.toByteArray();
        } catch (IOException ex) { // should never happen
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Close{");
        sb.append("requestId=").append(requestId);
        sb.append(", fd=").append(fd);
        sb.append('}');
        return sb.toString();
    }
}
