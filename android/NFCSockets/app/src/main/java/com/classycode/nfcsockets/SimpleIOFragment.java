package com.classycode.nfcsockets;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.classycode.nfcsockets.sockets.NFCSocket;

import java.io.IOException;
import java.net.Socket;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class SimpleIOFragment extends NFCSocketFragment {

    private static final String TAG = SimpleIOFragment.class.getSimpleName();

    private TextView logView;
    private ProgressBar progressBar;

    private EditText ipAddressField;
    private EditText portField;

    private StringBuilder log;

    private void addLog(final String line) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(line);
                log.append("\n");
                logView.setText(log.toString());
            }
        });
    }

    private void clearLog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log = new StringBuilder();
                logView.setText(log.toString());
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simple_io, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        logView = (TextView) view.findViewById(R.id.log_view);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);
        ipAddressField = (EditText) view.findViewById(R.id.ip_address);
        portField = (EditText) view.findViewById(R.id.port);

        clearLog();
    }

    @Override
    protected void onLinkEstablished() {
        clearLog();
        addLog("NFC link established");
        final String ipAddress;
        final Integer port;
        try {
            ipAddress = ipAddressField.getText().toString().trim();
            port = Integer.parseInt(portField.getText().toString().trim());
            if (ipAddress.isEmpty()) {
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
                    final Socket socket = new NFCSocket(ipAddress, port);
                    try {
                        addLog("Socket opened");
                        socket.getOutputStream().write("Hello\n".getBytes("UTF-8"));
                        addLog("Data written");
                        byte[] buf = new byte[256];
                        int len = socket.getInputStream().read(buf);
                        if (len > 0) {
                            addLog("Data received: " + new String(buf, 0, len, "UTF-8"));
                            return null;
                        } else {
                            throw new IOException("Read failed (returned 0 bytes)");
                        }
                    } finally {
                        socket.close();
                        addLog("Socket closed");
                    }
                } catch (IOException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Exception ex) {
                if (ex != null) {
                    addLog("Socket I/O failed: " + ex.getMessage());
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

}
