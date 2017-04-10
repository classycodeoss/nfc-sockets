package com.classycode.nfcsockets.messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class KeepAliveMessage extends NFCMessage {

    @Override
    public byte[] toApdu() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeByte(MSG_NFC);
            out.writeByte(MSG_NFC_LINK_KEEP_ALIVE);
            return buf.toByteArray();
        } catch (IOException ex) { // should never happen
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void fromApdu(byte[] apdu) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(apdu));
        if (in.readByte() != MSG_NFC || in.readByte() != MSG_NFC_LINK_KEEP_ALIVE) {
            throw new IllegalArgumentException("Malformed APDU");
        }
    }
}
