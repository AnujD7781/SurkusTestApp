package com.surkus.com.surkustestapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private WebView mWebViewPop;
    private FrameLayout mContainer;
    private Context mContext;

    private String siteUrl = "https://members-beta.surkus.com";
    private String siteDomain = "members-beta.surkus.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        webView  = (WebView) findViewById(R.id.webview);
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);

        webView.setWebViewClient(new MyCustomWebViewClient());
        webView.setWebChromeClient(new UriWebChromeClient());

        webView.loadUrl(siteUrl);
        mContext=this.getApplicationContext();
    }


    @Override
    public void onBackPressed() {
        if(webView.isFocused() && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private class MyCustomWebViewClient extends WebViewClient {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            String host = Uri.parse(url).getHost();

            if( url.startsWith("http:") || url.startsWith("https:") ) {
                if (host.equals(siteDomain)) {
                    if (mWebViewPop != null) {
                        mWebViewPop.setVisibility(View.GONE);
                        mContainer.removeView(mWebViewPop);
                        mWebViewPop = null;
                    }
                    return false;
                }

                if (host.contains("instagram.com") || host.contains("facebook.com")) {
                    return false;
                }
                // Otherwise, the link is not for a page on my site, so launch
                // another Activity that handles URLs
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
                return true;
            }

            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.d("onReceivedSslError", "onReceivedSslError");
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if(url.startsWith("https://www.facebook.com/dialog/oauth") ||
                    url.startsWith("https://members-beta.surkus.com/?code") ||
                    url.startsWith("https://members-beta.surkus.com/auth/instagram/callback")){
                if(mWebViewPop !=null)
                {
                    mWebViewPop.setVisibility(View.GONE);
                    mContainer.removeView(mWebViewPop);
                    mWebViewPop =null;
                }

                return;
            }
            super.onPageFinished(view, url);
        }
    }

    private class UriWebChromeClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            mWebViewPop = new WebView(mContext);

            WebSettings webSettings = mWebViewPop.getSettings();

            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
            webSettings.setSupportMultipleWindows(true);

            mWebViewPop.setVerticalScrollBarEnabled(false);
            mWebViewPop.setHorizontalScrollBarEnabled(false);
            mWebViewPop.setWebViewClient(new MyCustomWebViewClient());
            mWebViewPop.setWebChromeClient(new UriWebChromeClient());

            mWebViewPop.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mWebViewPop);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mWebViewPop);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            Log.d("onCloseWindow", "called");
        }

    }
}
