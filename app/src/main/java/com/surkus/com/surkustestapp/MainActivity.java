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
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.concurrent.ExecutionException;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private WebView mPopupWebView;
    private FrameLayout mContainer;
    private Context mContext;
    private Uri mCameraPhotoPath;
    private boolean mRequestedCameraPermissions = false;
    private ValueCallback<Uri[]> mOnFileSelected = null;
    private WebViewConfigurator mWebViewConfigurator = new WebViewConfigurator();

    private static final String SITE_URL = "https://members-beta.surkus.com";
    private static final String SITE_DOMAIN = "members-beta.surkus.com";

    private static String[] CAMERA_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final int CAMERA_PERMISSIONS_REQUEST_CODE = 1;
    private static final int FILE_CHOOSER_REQUEST_CODE = 2;

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
                        results = new Uri[] { Uri.parse(dataString) };
                    }
                }
            }

            mOnFileSelected.onReceiveValue(results);
            mOnFileSelected = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) {
            mRequestedCameraPermissions = true;
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected void requestCameraPermissions() {
        if (!mRequestedCameraPermissions) {
            ActivityCompat.requestPermissions(this, CAMERA_PERMISSIONS, CAMERA_PERMISSIONS_REQUEST_CODE);
        }
    }

    protected boolean hasCameraPermissions() {
        for (String permission : CAMERA_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }

        return true;
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
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            Uri uri = Uri.parse(url);

            if (uri.getPath() != null && uri.getPath().equals("/profile/photos")) {
                requestCameraPermissions();
            }

            super.doUpdateVisitedHistory(view, url, isReload);
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

            Intent[] intentCollection;

            if (takePictureIntent != null) {
                intentCollection = new Intent[] { takePictureIntent };
            } else {
                intentCollection = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);

            chooserIntent.putExtra(Intent.EXTRA_INTENT, fileChooserParams.createIntent());
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
