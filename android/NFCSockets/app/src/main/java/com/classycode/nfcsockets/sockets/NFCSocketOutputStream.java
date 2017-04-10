package com.classycode.nfcsockets.sockets;

import android.support.annotation.NonNull;
import android.util.Log;

import com.classycode.nfcsockets.Constants;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
class NFCSocketOutputStream extends OutputStream {

    private static final String TAG = Constants.LOG_TAG;

    private final NFCSocket nfcSocket;
    private boolean isClosed;

    NFCSocketOutputStream(NFCSocket nfcSocket) {
        this.nfcSocket = nfcSocket;
        isClosed = false;
    }

    @Override
    public void write(int b) throws IOException {
        ensureNotClosed();
        final byte[] wrappedByte = new byte[]{(byte) b};
        nfcSocket.writeInternal(wrappedByte, 0, wrappedByte.length);
    }

    @Override
    public void write(@NonNull byte[] b) throws IOException {
        ensureNotClosed();
        nfcSocket.writeInternal(b, 0, b.length);
    }

    @Override
    public void write(@NonNull byte[] b, int off, int len) throws IOException {
        ensureNotClosed();
        nfcSocket.writeInternal(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        ensureNotClosed();
        Log.d(TAG, "NFCSocketOutputStream.flush() is a no-op");
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;

            // according to Socket.getOutputStream documentation, closing the OutputStream
            // should close the socket
            nfcSocket.close();
        } else {
            Log.w(TAG, "Stream already closed");
        }
    }

    private void ensureNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("OutputStream is closed");
        }
    }
}
