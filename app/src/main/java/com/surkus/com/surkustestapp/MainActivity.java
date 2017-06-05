package com.surkus.com.surkustestapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private WebView mPopupWebView;
    private FrameLayout mContainer;
    private Context mContext;
    private Uri mCameraPhotoPath;
    private ValueCallback<Uri[]> mOnFileSelected = null;
    private WebViewConfigurator mWebViewConfigurator = new WebViewConfigurator();

    private static final String SITE_URL = "https://members-beta.surkus.com";
    private static final String SITE_DOMAIN = "members-beta.surkus.com";
    private static final int FILE_CHOOSER_REQUEST_CODE = 999;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        mWebView = (WebView) findViewById(R.id.webview);
        mContainer = (FrameLayout) findViewById(R.id.webview_frame);

        mWebViewConfigurator.Configure(mWebView);

        mWebView.loadUrl(SITE_URL);

        mContext = this.getApplicationContext();
    }

    @Override
    public void onBackPressed() {
        if(mWebView.isFocused() && mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST_CODE && mOnFileSelected != null) {
            Uri[] results = null;

            if (resultCode == RESULT_OK) {
                if (data == null) {
                    if (mCameraPhotoPath != null) {
                        results = new Uri[] { mCameraPhotoPath };

                        mCameraPhotoPath = null;
                    }
                } else {
                    String dataString = data.getDataString();

                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }

            mOnFileSelected.onReceiveValue(results);
            mOnFileSelected = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String uriScheme = uri.getScheme();

            if (uriScheme.equals("http") || uriScheme.equals("https")) {
                if (uri.getHost().equals(SITE_DOMAIN)) {
                    removePopup();

                    return false;
                }

                if (isAuthHost(uri.getHost())) {
                    return false;
                }

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);

                return true;
            }

            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (isCallbackUrl(url)) {
                removePopup();
            } else {
                super.onPageFinished(view, url);
            }
        }

        private boolean isCallbackUrl(String url) {
            return url.startsWith("https://www.facebook.com/dialog/oauth") ||
                url.startsWith("https://members-beta.surkus.com/?code") ||
                url.startsWith("https://members-beta.surkus.com/auth/instagram/callback");
        }

        private boolean isAuthHost(String host) {
            return host.contains("instagram.com") || host.contains("facebook.com");
        }

        private void removePopup() {
            if (mPopupWebView != null) {
                mPopupWebView.setVisibility(View.GONE);
                mContainer.removeView(mPopupWebView);
                mPopupWebView = null;
            }
        }
    }

    private class CustomWebClient extends WebChromeClient {

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            mPopupWebView = new WebView(mContext);

            mWebViewConfigurator.Configure(mPopupWebView);

            mPopupWebView.setVerticalScrollBarEnabled(false);
            mPopupWebView.setHorizontalScrollBarEnabled(false);

            mPopupWebView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            mContainer.addView(mPopupWebView);
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(mPopupWebView);
            resultMsg.sendToTarget();

            return true;
        }

        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         WebChromeClient.FileChooserParams fileChooserParams) {
            if (mOnFileSelected != null) {
                mOnFileSelected.onReceiveValue(null);
            }

            mOnFileSelected = filePathCallback;

            Intent takePictureIntent = null;

            if (hasCameraPermissions()) {
                mCameraPhotoPath = generateCameraPhotoPath();

                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCameraPhotoPath);
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");

            Intent[] intentCollection;

            if (takePictureIntent != null) {
                intentCollection = new Intent[] { takePictureIntent };
            } else {
                intentCollection = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);

            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Selector");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentCollection);

            startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE);

            return true;
        }

        private Uri generateCameraPhotoPath() {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            String photoPath = "file:" + storageDir + "/" + imageFileName + ".jpg";

            return Uri.parse(photoPath);
        }

        private boolean hasCameraPermissions() {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private class WebViewConfigurator {
        public void Configure(WebView view) {
            WebSettings settings = view.getSettings();

            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);
            settings.setSupportMultipleWindows(true);

            view.setWebViewClient(new CustomWebViewClient());
            view.setWebChromeClient(new CustomWebClient());
        }
    }
}
