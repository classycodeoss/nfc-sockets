package com.classycode.nfcsockets.okhttp;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.classycode.nfcsockets.util.DummyHostnameVerifier;
import com.classycode.nfcsockets.util.TrustAllCertificates;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.CacheControl;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * {@link WebViewClient} implementation that intercepts all requests and uses OkHttp to fetch them
 * via an NFC socket.
 *
 * @author Alex Suzuki, Classy Code GmbH, 2017
 */
public class OkHttpWebViewClient extends WebViewClient {

    private static final String TAG = OkHttpWebViewClient.class.getSimpleName();

    private static final boolean TRUST_ALL_CERTIFICATES = false;

    private final OkHttpClient client;

    public OkHttpWebViewClient(SocketFactory socketFactory) {
        try {
            final ConnectionPool noPool = new ConnectionPool(0, 1, TimeUnit.SECONDS);
            OkHttpClient.Builder builder = new OkHttpClient.Builder().socketFactory(socketFactory)
                    // override DNS
                    .dns(new OkDns())
                    // disable connection pooling
                    .connectionPool(noPool)
                    // no timeouts, we don't support them yet
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .writeTimeout(0, TimeUnit.MILLISECONDS)
                    .connectTimeout(0, TimeUnit.MILLISECONDS);
            if (TRUST_ALL_CERTIFICATES) { // for development purposes
                final X509TrustManager trustManager = new TrustAllCertificates();
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
                builder.hostnameVerifier(new DummyHostnameVerifier());
            }
            client = builder.build();
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        return loadResponse(view, url);
    }

    @SuppressLint("NewApi")
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        return loadResponse(view, request.getUrl().toString());
    }

    private WebResourceResponse loadResponse(WebView view, String url) {
        Log.i(TAG, "Loading url: " + url);

        // suppress favicon requests as we don't display them anywhere
        if (url.endsWith("/favicon.ico")) {
            return new WebResourceResponse("image/png", null, null);
        }

        try {
            final Request okReq = new Request.Builder().url(url)
                    .cacheControl(new CacheControl.Builder().noCache().build())
                    .build();

            final long startMillis = System.currentTimeMillis();
            final Response okResp = client.newCall(okReq).execute();
            final long dtMillis = System.currentTimeMillis() - startMillis;
            Log.d(TAG, "Got response: " + okResp + " after " + dtMillis + "ms");
            return okHttpResponseToWebResourceResponse(okResp);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load request: " + url, e);
            try {
                final String errorPage = IOUtils.toString(view.getContext().getAssets().open("request_failed.html"), "UTF-8")
                        .replace("__ERROR_MESSAGE__", e.getMessage());
                final InputStream respStream = new ByteArrayInputStream(errorPage.getBytes("UTF-8"));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    return new WebResourceResponse("text/html", "UTF-8", 500, "FAIL", null, respStream);
                } else {
                    return new WebResourceResponse("text/html", "UTF-8", respStream);
                }
            } catch (IOException ex) {
                Log.e(TAG, "Loading error page from assets failed", ex); // should never happen
                return null;
            }
        }
    }

    /**
     * Convert OkHttp {@link Response} into a {@link WebResourceResponse}
     *
     * @param resp The OkHttp {@link Response}
     * @return The {@link WebResourceResponse}
     */
    private WebResourceResponse okHttpResponseToWebResourceResponse(Response resp) {
        final String contentTypeValue = resp.header("Content-Type");
        if (contentTypeValue != null) {
            if (contentTypeValue.indexOf("charset=") > 0) {
                final String[] contentTypeAndEncoding = contentTypeValue.split("; ");
                final String contentType = contentTypeAndEncoding[0];
                final String charset = contentTypeAndEncoding[1].split("=")[1];
                return new WebResourceResponse(contentType, charset, resp.body().byteStream());
            } else {
                return new WebResourceResponse(contentTypeValue, null, resp.body().byteStream());
            }
        } else {
            return new WebResourceResponse("application/octet-stream", null, resp.body().byteStream());
        }
    }
}
