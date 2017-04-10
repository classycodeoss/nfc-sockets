package com.classycode.nfcsockets;

import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;


/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class NFCSocketActivity extends AppCompatActivity {

    private static final String TAG = NFCSocketActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_sockets);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        final TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText(R.string.tab_simpleio));
        tabs.addTab(tabs.newTab().setText(R.string.tab_https));
        tabs.addTab(tabs.newTab().setText(R.string.tab_ssh));
        tabs.setTabGravity(TabLayout.GRAVITY_FILL);
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SimpleIOFragment()).commit();
                } else if (tab.getPosition() == 1) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new WebFragment()).commit();
                } else if (tab.getPosition() == 2) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SSHFragment()).commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new SimpleIOFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkForNFC();
    }

    private void checkForNFC() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        if (!nfcAdapter.isEnabled()) {
            Log.w(TAG, "NFC is not enabled");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.error_nfc_disabled_title);
            builder.setMessage(R.string.error_nfc_disabled_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                }
            });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
        }
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment instanceof NFCSocketFragment) {
            if (((NFCSocketFragment) fragment).onBackButtonPressed()) { // fragment handled it
                return;
            }
        }

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            super.onBackPressed();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }
}
