package com.classycode.nfcsockets.minidns;

import com.classycode.nfcsockets.sockets.NFCSocket;

import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import de.measite.minidns.source.NetworkDataSource;

/**
 * A {@link NetworkDataSource} implementation that uses NFC sockets.
 *
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCDnsSource extends NetworkDataSource {

    @Override
    protected Socket createSocket() {
        return new NFCSocket();
    }

    @Override
    protected DatagramSocket createDatagramSocket() throws SocketException {
        return super.createDatagramSocket();
    }
}
