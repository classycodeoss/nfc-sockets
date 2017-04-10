package com.classycode.nfcsockets.sockets;

import android.support.annotation.NonNull;
import android.util.Log;

import com.classycode.nfcsockets.Constants;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
class NFCSocketInputStream extends InputStream {

    private static final String TAG = Constants.LOG_TAG;

    private final NFCSocket nfcSocket;
    private boolean isClosed;

    NFCSocketInputStream(NFCSocket nfcSocket) {
        this.nfcSocket = nfcSocket;
        this.isClosed = false;
    }

    @Override
    public int read(@NonNull byte[] b) throws IOException {
        ensureNotClosed();
        return nfcSocket.readInternal(b, 0, b.length);
    }

    @Override
    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        ensureNotClosed();
        return nfcSocket.readInternal(b, off, len);
    }

    @Override
    public int read() throws IOException {
        ensureNotClosed();
        byte[] ar = new byte[1];
        int res = nfcSocket.readInternal(ar, 0, 1);
        if (res > 0) {
            return ar[0];
        }
        else {
            return -1;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        throw new IllegalStateException("skip not implemented");
    }

    @Override
    public int available() throws IOException {
        Log.d(TAG, "NFCSocketInputStream.available()");
        return 0;
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;

            // according to Socket.getInputStream documentation, closing the OutputStream
            // should close the socket
            nfcSocket.close();
        }
        else {
            Log.w(TAG, "Stream already closed");
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new IllegalStateException("mark not supported");
    }

    @Override
    public synchronized void reset() throws IOException {
        throw new IOException("mark not supported");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void ensureNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("OutputStream is closed");
        }
    }
}
