package com.classycode.nfcsockets;

import android.content.Context;
import android.content.Intent;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.classycode.nfcsockets.messages.KeepAliveMessage;
import com.classycode.nfcsockets.messages.LinkTerminateMessage;
import com.classycode.nfcsockets.messages.Message;
import com.classycode.nfcsockets.messages.SocketMessage;
import com.classycode.nfcsockets.messages.SocketRequest;

import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCSocketApduService extends HostApduService {

    public static ConcurrentLinkedQueue<SocketRequest> outboundQueue = new ConcurrentLinkedQueue<>();

    public static final int MAX_APDU_PAYLOAD_SIZE = 255;

    public static final String BROADCAST_INTENT_LINK_ESTABLISHED = "LINK_ESTABLISHED";
    public static final String BROADCAST_INTENT_LINK_DEACTIVATED = "LINK_DEACTIVATED";

    private static final String TAG = Constants.LOG_TAG;

    // the SELECT AID APDU issued by the terminal
    // our AID is 0xF0ABCDFF0000
    private static final byte[] SELECT_AID_COMMAND = {
            (byte) 0x00, // Class
            (byte) 0xA4, // Instruction
            (byte) 0x04, // Parameter 1
            (byte) 0x00, // Parameter 2
            (byte) 0x06, // length
            (byte) 0xF0, (byte) 0xAB, (byte) 0xCD, (byte) 0xFF, (byte) 0x00, (byte) 0x00
    };

    // OK status sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_RESPONSE_OK = {(byte) 0x90, (byte) 0x00};

    private boolean isProcessing;

    @Subscribe
    public void onSocketRequest(SocketRequest socketRequest) {
        outboundQueue.add(socketRequest);
        Log.d(TAG, "Queued outbound message: " + socketRequest);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isProcessing = false;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (!isProcessing) { // new connection
            Log.i(TAG, "Link activated");
            isProcessing = true;
            SocketEventBus.instance.register(this);
        }

        if (Arrays.equals(SELECT_AID_COMMAND, commandApdu)) { // NFC Terminal selected us
            notifyLinkEstablished();
            return SELECT_RESPONSE_OK;
        } else {
            final Message message;
            try {
                message = Message.parseMessage(commandApdu);
            } catch (IOException e) {
                Log.e(TAG, "Failed to parse inbound message", e);
                return new LinkTerminateMessage().toApdu();
            }
            if (message instanceof KeepAliveMessage) {

                // send next pending message
                final SocketRequest pendingSocketRequest = outboundQueue.poll();
                if (pendingSocketRequest != null) {
                    return pendingSocketRequest.toApdu();
                }
                else {
                    return new KeepAliveMessage().toApdu();
                }
            }
            else if (message instanceof LinkTerminateMessage) {
                return new LinkTerminateMessage().toApdu();
            }
            else if (message instanceof SocketMessage) {
                Log.d(TAG, "Received socket response: " + message);
                SocketEventBus.instance.post(message);

                // send next pending message
                final SocketRequest pendingSocketRequest = outboundQueue.poll();
                if (pendingSocketRequest != null) {
                    return pendingSocketRequest.toApdu();
                }
                else {
                    return new KeepAliveMessage().toApdu();
                }
            }
            else {
                throw new IllegalStateException("Unknown message received");
            }
        }
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "Link deactivated: " + reason);

        SocketEventBus.instance.unregister(this);

        isProcessing = false;
        notifyLinkDeactivated(reason);
    }

    private void notifyLinkEstablished() {
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(BROADCAST_INTENT_LINK_ESTABLISHED));
    }

    private void notifyLinkDeactivated(int reason) {
        Intent intent = new Intent(BROADCAST_INTENT_LINK_DEACTIVATED);
        intent.putExtra("reason", reason);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

}
