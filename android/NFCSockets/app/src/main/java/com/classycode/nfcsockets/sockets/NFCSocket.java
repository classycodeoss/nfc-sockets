package com.classycode.nfcsockets.sockets;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.classycode.nfcsockets.Constants;
import com.classycode.nfcsockets.SocketEventBus;
import com.classycode.nfcsockets.messages.Close;
import com.classycode.nfcsockets.messages.CloseResponse;
import com.classycode.nfcsockets.messages.Connect;
import com.classycode.nfcsockets.messages.ConnectResponse;
import com.classycode.nfcsockets.messages.Recv;
import com.classycode.nfcsockets.messages.RecvResponse;
import com.classycode.nfcsockets.messages.Send;
import com.classycode.nfcsockets.messages.SendResponse;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCSocket extends Socket {

    private static final String TAG = Constants.LOG_TAG;

    private static final int ILLEGAL_FD = -1;

    private int remoteFd;

    private Connect pendingConnect;
    private ConnectResponse connectResponse;

    private Recv pendingRecv;
    private RecvResponse recvResponse;

    private Send pendingSend;
    private SendResponse sendResponse;

    private Close pendingClose;
    private CloseResponse closeResponse;

    private InetSocketAddress socketAddress;

    private NFCSocketInputStream in;
    private NFCSocketOutputStream out;

    public NFCSocket() {
        try {
            init(null, 0, false);
        } catch (IOException e) {
            throw new IllegalStateException("Creation of unconnected socket threw exception", e);
        }
    }

    public NFCSocket(String host, int port) throws IOException {
        init(host, port, true);
    }

    public NFCSocket(InetAddress address, int port) throws IOException {
        init(address.getHostAddress(), port, true);
    }

    public NFCSocket(String host, int port, InetAddress localAddr, int localPort) throws IOException {
        init(host, port, true);
    }

    public NFCSocket(InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        init(address.getHostAddress(), port, true);
    }

    private void init(String host, int port, boolean connect) throws IOException {
        Log.d(TAG, "NFCSocket.init()");

        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            throw new IllegalStateException("Can not use NFC socket on main thread");
        }

        remoteFd = ILLEGAL_FD;
        SocketEventBus.instance.register(this);
        try {
            if (connect) {
                doConnect(host, port);
            }
        } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for NFC socket", ex);
        }
    }

    private void doConnect(String host, int port) throws InterruptedException, IOException {
        Log.d(TAG, "NFCSocket.doConnect(" + host + ", " + port + ")");

        try {
            pendingConnect = new Connect(host, port);
            SocketEventBus.instance.post(pendingConnect);

            // now wait
            synchronized (this) {
                while (connectResponse == null) {
                    wait();
                }
                pendingConnect = null;
            }

            if (!connectResponse.isSuccess()) {
                throw new IOException("connect() returned: " + ((ConnectResponse) connectResponse).getRes());
            } else {
                remoteFd = connectResponse.getRes();
                in = new NFCSocketInputStream(this);
                out = new NFCSocketOutputStream(this);
            }
            socketAddress = new InetSocketAddress(host, port);
        } finally {
            connectResponse = null;
        }
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        Log.d(TAG, "NFCSocket.connect()");
        InetSocketAddress inetSocketAddress = (InetSocketAddress) endpoint;
        try {
            doConnect(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for NFC socket", ex);
        }
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        Log.d(TAG, "NFCSocket.connect()");
        InetSocketAddress inetSocketAddress = (InetSocketAddress) endpoint;
        try {
            doConnect(inetSocketAddress.getHostName(), inetSocketAddress.getPort());
        } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for NFC socket", ex);
        }
    }

    int readInternal(@NonNull byte[] b, int off, int len) throws IOException {
        pendingRecv = new Recv(remoteFd, len);
        try {
            SocketEventBus.instance.post(pendingRecv);
            synchronized (this) {
                while (recvResponse == null) {
                    wait();
                }
                pendingRecv = null;
            }
            if (recvResponse.isSuccess()) {
                Log.d(TAG, "Recv received " + recvResponse.getRes() + " bytes");
                System.arraycopy(recvResponse.getData(), 0, b, off, recvResponse.getRes());
                return recvResponse.getRes();
            } else {
                throw new IOException("recv() failed: " + recvResponse.getRes());
            }
        } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for NFC socket", ex);
        } finally {
            recvResponse = null;
        }
    }

    void writeInternal(byte[] b, int off, int len) throws IOException {
        int numWrites = len / Send.MAX_DATA_SIZE_PER_WRITE;
        if (len % Send.MAX_DATA_SIZE_PER_WRITE != 0) {
            numWrites += 1;
        }
        Log.d(TAG, "Attempting to send " + len + " bytes of data in " + numWrites + " writes");
        for (int i = 0; i < numWrites; i++) {
            final int packetOffset = off + i * Send.MAX_DATA_SIZE_PER_WRITE;
            final int packetLength;
            if (i == numWrites - 1) { // last one
                if (len % Send.MAX_DATA_SIZE_PER_WRITE == 0) {
                    packetLength = Send.MAX_DATA_SIZE_PER_WRITE;
                } else {
                    packetLength = len % Send.MAX_DATA_SIZE_PER_WRITE;
                }
            } else {
                packetLength = Send.MAX_DATA_SIZE_PER_WRITE;
            }

            Log.d(TAG, "Sending packet " + i + " containing " + packetLength + " bytes of data");

            pendingSend = new Send(remoteFd, b, packetOffset, packetLength);
            try {
                SocketEventBus.instance.post(pendingSend);
                synchronized (this) {
                    while (sendResponse == null) {
                        wait();
                    }
                    pendingSend = null;
                }

                if (!sendResponse.isSuccess()) {
                    throw new IOException("send() failed: " + sendResponse.getRes());
                }
                else {
                    Log.d(TAG, "Send sent " + sendResponse.getRes() + " bytes");
                }
            } catch (InterruptedException ex) {
                throw new IOException("Interrupted while waiting for NFC socket", ex);
            } finally {
                sendResponse = null;
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (remoteFd == ILLEGAL_FD) {
            Log.w(TAG, "Socket already closed");
            return;
        }

        pendingClose = new Close(remoteFd);
        try {
            SocketEventBus.instance.post(pendingClose);
            synchronized (this) {
                while (closeResponse == null) {
                    wait();
                }
                pendingClose = null;
            }
            if (!closeResponse.isSuccess()) {
                throw new IOException("close() failed: " + closeResponse.getRes());
            }
            else {
                Log.d(TAG, "Closed socket with remoteFd: " + remoteFd);
                remoteFd = ILLEGAL_FD;
            }
        } catch (InterruptedException ex) {
            throw new IOException("Interrupted while waiting for NFC socket", ex);
        } finally {
            closeResponse = null;
        }
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public InetAddress getInetAddress() {
        if (socketAddress == null) {
            return null;
        }
        return socketAddress.getAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return null; // TODO
    }

    @Override
    public int getPort() {
        if (socketAddress == null) {
            return 0;
        }
        return socketAddress.getPort();
    }

    @Override
    public int getLocalPort() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return socketAddress;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public SocketChannel getChannel() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return in;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return out;
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return false; // TODO
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
    }

    @Override
    public int getSoLinger() throws SocketException {
        return -1;
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return false;
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        Log.e(TAG, "Timeouts are not implemented, ignoring setSoTimeout: " + timeout);
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return 0;
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return 2048; // TODO
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return 2048; // TODO
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return true;
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return 0x2; // IP_TOS_LOWCOST
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return false;
    }

    @Override
    public void shutdownInput() throws IOException {
    }

    @Override
    public void shutdownOutput() throws IOException {
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NFCSocket{");
        sb.append("remoteFd=").append(remoteFd);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean isConnected() {
        return remoteFd != ILLEGAL_FD;
    }

    @Override
    public boolean isBound() {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public boolean isClosed() {
        return remoteFd == ILLEGAL_FD;
    }

    @Override
    public boolean isInputShutdown() {
        return false;
    }

    @Override
    public boolean isOutputShutdown() {
        return false;
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }

    @Subscribe
    public void onConnectResponse(ConnectResponse connectResponse) {
        synchronized (this) {
            if (pendingConnect != null && connectResponse.getInReplyTo() == pendingConnect.getRequestId()) {
                this.connectResponse = connectResponse;
                notifyAll();
            }
        }
    }

    @Subscribe
    public void onCloseResponse(CloseResponse closeResponse) {
        synchronized (this) {
            if (pendingClose != null && closeResponse.getInReplyTo() == pendingClose.getRequestId()) {
                this.closeResponse = closeResponse;
                notifyAll();
            }
        }
    }

    @Subscribe
    public synchronized void onSendResponse(SendResponse sendResponse) {
        synchronized (this) {
            if (pendingSend != null && sendResponse.getInReplyTo() == pendingSend.getRequestId()) {
                this.sendResponse = sendResponse;
                notifyAll();
            }
        }
    }

    @Subscribe
    public void onRecvResponse(RecvResponse recvResponse) {
        synchronized (this) {
            if (pendingRecv != null && recvResponse.getInReplyTo() == pendingRecv.getRequestId()) {
                this.recvResponse = recvResponse;
                notifyAll();
            }
        }
    }

}
