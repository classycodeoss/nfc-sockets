package com.classycode.nfcsockets;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.classycode.nfcsockets.sockets.NFCSocketFactory;
import com.classycode.nfcsockets.jsch.SSHCommandExecutor;
import com.jcraft.jsch.JSchException;

import java.io.IOException;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class SSHFragment extends NFCSocketFragment implements SSHCommandExecutor.SSHCommandExecutionListener {

    private static final String TAG = SSHFragment.class.getSimpleName();

    private TextView logView;
    private ProgressBar progressBar;

    private EditText ipAddressField;
    private EditText portField;
    private EditText usernameField;
    private EditText passwordField;
    private EditText commandField;

    private StringBuilder log;

    private void addLog(String line) {
        log.append(line);
        log.append("\n");
        logView.setText(log.toString());
    }

    private void clearLog() {
        log = new StringBuilder();
        logView.setText(log.toString());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssh, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logView = (TextView) view.findViewById(R.id.log_view);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
        ipAddressField = (EditText) view.findViewById(R.id.ip_address);
        portField = (EditText) view.findViewById(R.id.port);
        usernameField = (EditText) view.findViewById(R.id.ssh_username);
        passwordField = (EditText) view.findViewById(R.id.ssh_password);
        commandField = (EditText) view.findViewById(R.id.ssh_command);

        clearLog();
    }

    @Override
    protected void onLinkEstablished() {
        clearLog();
        addLog("NFC link established");
        final String ipAddress;
        final Integer port;
        final String username;
        final String password;
        final String command;
        try {
            ipAddress = ipAddressField.getText().toString().trim();
            port = Integer.parseInt(portField.getText().toString().trim());
            username = usernameField.getText().toString().trim();
            password = passwordField.getText().toString().trim();
            command = commandField.getText().toString().trim();
            if (ipAddress.isEmpty() || username.isEmpty() || password.isEmpty() || command.isEmpty()) {
                addLog("Input validation failed");
                return;
            }
        } catch (NumberFormatException ex) {
            addLog("Port must be numeric");
            return;
        }

        new AsyncTask<Void, Void, Exception>() {

            @Override
            protected void onPreExecute() {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    final SSHCommandExecutor sshExec = new SSHCommandExecutor(new NFCSocketFactory());
                    sshExec.setCommandExecutionListener(SSHFragment.this);
                    sshExec.executeCommand(ipAddress, port, username, password, command);
                    return null;
                } catch (IOException e) {
                    Log.e(TAG, "SSH session failed", e);
                    return e;
                } catch (JSchException e) {
                    Log.e(TAG, "SSH session failed", e);
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception ex) {
                if (ex != null) {
                    addLog("SSH session failed: " + ex.getMessage());
                }
                progressBar.setVisibility(View.INVISIBLE);
            }
        }.execute();
    }

    @Override
    protected void onLinkDeactivated() {
        addLog("NFC link terminated");
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onSessionConnected() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() != null) {
                    addLog("SSH session connected");
                }
            }
        });
    }

    @Override
    public void onSessionDisconnected() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() != null) {
                    addLog("SSH session disconnected");
                }
            }
        });
    }

    @Override
    public void onCommandOutput(final String output) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (getView() != null) {
                    addLog("Command output: " + output);
                }
            }
        });
    }
}
