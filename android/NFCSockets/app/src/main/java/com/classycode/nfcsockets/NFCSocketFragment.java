package com.classycode.nfcsockets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public abstract class NFCSocketFragment extends Fragment {

    private static final String TAG = NFCSocketFragment.class.getSimpleName();

    protected LocalBroadcastManager lbm;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lbm = LocalBroadcastManager.getInstance(getContext());
    }

    @Override
    public void onResume() {
        super.onResume();

        lbm.registerReceiver(linkEstablishedReceiver, new IntentFilter(NFCSocketApduService.BROADCAST_INTENT_LINK_ESTABLISHED));
        lbm.registerReceiver(linkDeactivatedReceiver, new IntentFilter(NFCSocketApduService.BROADCAST_INTENT_LINK_DEACTIVATED));
        Log.d(TAG, "Fragment resumed: " + getClass().getSimpleName());
    }

    @Override
    public void onPause() {
        lbm.unregisterReceiver(linkEstablishedReceiver);
        lbm.unregisterReceiver(linkDeactivatedReceiver);

        super.onPause();
        Log.d(TAG, "Fragment paused: " + getClass().getSimpleName());
    }

    private BroadcastReceiver linkEstablishedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            onLinkEstablished();
        }
    };

    private BroadcastReceiver linkDeactivatedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onLinkDeactivated();
        }
    };

    protected abstract void onLinkEstablished();

    protected abstract void onLinkDeactivated();

    public boolean onBackButtonPressed() {
        return false;
    }
}
