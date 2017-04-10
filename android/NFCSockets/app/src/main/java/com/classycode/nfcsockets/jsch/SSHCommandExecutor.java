package com.classycode.nfcsockets.jsch;

import android.util.Log;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

/**
 * Opens an SSH connection authenticated by username/password, performs a single command, and returns its output.
 *
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class SSHCommandExecutor {

    public interface SSHCommandExecutionListener {

        void onSessionConnected();

        void onCommandOutput(String output);

        void onSessionDisconnected();
    }

    private static final String TAG = SSHCommandExecutor.class.getSimpleName();

    private SSHCommandExecutionListener commandExecutionListener;
    private SocketFactory socketFactory;

    public SSHCommandExecutor(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public void setCommandExecutionListener(SSHCommandExecutionListener commandExecutionListener) {
        this.commandExecutionListener = commandExecutionListener;
    }

    public void executeCommand(String hostname, int port, String username, String password, String command) throws IOException, JSchException {
        final JSch jSch = new JSch();
        final Session sshSess = jSch.getSession(username, hostname, port);
        sshSess.setPassword(password);
        sshSess.setConfig("StrictHostKeyChecking", "no");
        sshSess.setConfig("PreferredAuthentications", "password");
        sshSess.setSocketFactory(new com.jcraft.jsch.SocketFactory() {
            @Override
            public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
                return socketFactory.createSocket(host, port);
            }

            @Override
            public InputStream getInputStream(Socket socket) throws IOException {
                return socket.getInputStream();
            }

            @Override
            public OutputStream getOutputStream(Socket socket) throws IOException {
                return socket.getOutputStream();
            }
        });
        sshSess.connect();
        Log.i(TAG, "SSH session connected");
        if (commandExecutionListener != null) {
            commandExecutionListener.onSessionConnected();
        }

        final ChannelExec channelExec = (ChannelExec) sshSess.openChannel("exec");
        channelExec.setCommand(command);
        channelExec.setOutputStream(null);
        channelExec.connect();
        Log.i(TAG, "SSH exec channel connected");

        final String output = IOUtils.toString(channelExec.getInputStream(), "ASCII");
        Log.i(TAG, "SSH command output: " + output);
        if (commandExecutionListener != null) {
            commandExecutionListener.onCommandOutput(output);
        }

        channelExec.disconnect();
        sshSess.disconnect();

        Log.i(TAG, "SSH exec channel disconnected");
        if (commandExecutionListener != null) {
            commandExecutionListener.onSessionDisconnected();
        }
    }
}
