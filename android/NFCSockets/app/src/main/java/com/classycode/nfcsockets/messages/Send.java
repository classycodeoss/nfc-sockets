package com.classycode.nfcsockets.messages;

import com.classycode.nfcsockets.NFCSocketApduService;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class Send extends SocketRequest {

    // payload size is limited by max APDU size, minus overhead (type, subtype, handle)
    public static int MAX_DATA_SIZE_PER_WRITE = NFCSocketApduService.MAX_APDU_PAYLOAD_SIZE - 1 - 1 - 4;

    private final int fd;
    private final byte[] data;
    private final int offset;
    private final int len;

    public Send(int fd, byte[] data, int offset, int len) {
        if (len > MAX_DATA_SIZE_PER_WRITE) {
            throw new IllegalArgumentException("Send request is too large: " + len + " > " + MAX_DATA_SIZE_PER_WRITE);
        }
        this.fd = fd;
        this.data = data;
        this.offset = offset;
        this.len = len;
    }

    @Override
    public byte[] toApdu() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(MSG_SOCKET);
            out.writeByte(MSG_SOCKET_SEND);
            out.writeInt(requestId);
            out.writeInt(fd);
            out.write(data, offset, len);
            return buf.toByteArray();
        } catch (IOException ex) { // should never happen
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Send{");
        sb.append("requestId=").append(requestId);
        sb.append(", fd=").append(fd);
        sb.append(", offset=").append(offset);
        sb.append(", len=").append(len);
        sb.append('}');
        return sb.toString();
    }
}
