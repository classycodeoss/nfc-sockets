package com.classycode.nfcsockets.sockets;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCSocketFactory extends SocketFactory {

    public NFCSocketFactory() {
    }

    @Override
    public Socket createSocket() throws IOException {
        return new NFCSocket();
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return new NFCSocket(host, port);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
        return new NFCSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return new NFCSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return new NFCSocket(address, port, localAddress, localPort);
    }
}
