package com.classycode.nfcsockets;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.classycode.nfcsockets.http.OkHttpWebViewClient;
import com.classycode.nfcsockets.sockets.NFCSocketFactory;

import java.net.MalformedURLException;
import java.net.URL;

import javax.net.SocketFactory;

/**
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class WebFragment extends NFCSocketFragment {

    private static final String TAG = WebFragment.class.getSimpleName();

    private WebView webView;
    private EditText urlField;
    private ProgressBar progressBar;

    private class WebViewClient extends OkHttpWebViewClient {

        public WebViewClient(SocketFactory socketFactory) {
            super(socketFactory);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.INVISIBLE);
            Log.i(TAG, "Page loading finished: " + url);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.i(TAG, "Page loading started: " + url);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_web, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        webView = (WebView) view.findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        urlField = (EditText) view.findViewById(R.id.url);
        progressBar = (ProgressBar) view.findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.INVISIBLE);

        webView.setWebViewClient(new WebViewClient(new NFCSocketFactory()));
    }

    @Override
    protected void onLinkEstablished() {
        final URL url;
        try {
            url = new URL(urlField.getText().toString().trim());
            webView.loadUrl(url.toString());
        } catch (MalformedURLException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.error_malformed_url_title);
            builder.setMessage(R.string.error_malformed_url_message);
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onLinkDeactivated() {
        webView.stopLoading();
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean onBackButtonPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
