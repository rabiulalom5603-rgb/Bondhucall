package com.bondhucall.app;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private PermissionRequest pendingRequest;
    private static final int PERMISSION_REQUEST = 41;
    private String[] neededPermissions = new String[] {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16, 24, 39));
        getWindow().setNavigationBarColor(Color.rgb(16, 24, 39));
        showHome();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(10), dp(8), dp(10), dp(8));
        return t;
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(24), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(16, 24, 39));

        TextView logo = text("📞", 34, Color.WHITE);
        root.addView(logo);
        TextView title = text("বন্ধু কল", 27, Color.rgb(54, 211, 153));
        root.addView(title);
        root.addView(text("ইন্টারনেটে অডিও ও ভিডিও কল", 15, Color.LTGRAY));

        TextView guide = text("প্রথমে Daily.co-তে একটি রুম তৈরি করুন।\nতারপর রুমের HTTPS URL এখানে দিন।\nদুইজন একই রুমের URL ব্যবহার করবেন।", 14, Color.WHITE);
        guide.setPadding(dp(8), dp(18), dp(8), dp(18));
        root.addView(guide);

        EditText urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://your-room.daily.co/room");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setTextSize(15);
        urlInput.setPadding(dp(12), dp(12), dp(12), dp(12));
        urlInput.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(54, 211, 153)));
        root.addView(urlInput, new LinearLayout.LayoutParams(-1, dp(54)));

        Button join = new Button(this);
        join.setText("অডিও / ভিডিও কলে যোগ দিন");
        root.addView(join, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView note = text("কলের জন্য ইন্টারনেট, রুম URL এবং ক্যামেরা/মাইক্রোফোনের অনুমতি লাগবে।", 12, Color.LTGRAY);
        root.addView(note);

        join.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (!url.startsWith("https://") || !url.contains(".daily.co/")) {
                Toast.makeText(this, "Daily রুমের সঠিক HTTPS URL দিন", Toast.LENGTH_LONG).show();
                return;
            }
            hideKeyboard();
            openCall(url);
        });

        setContentView(root);
    }

    private void openCall(String url) {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(8, 13, 21));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean hasCamera = Build.VERSION.SDK_INT < 23 ||
                        checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
                    boolean hasMic = Build.VERSION.SDK_INT < 23 ||
                        checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    if (hasCamera && hasMic) {
                        request.grant(request.getResources());
                    } else {
                        pendingRequest = request;
                        if (Build.VERSION.SDK_INT >= 23) {
                            requestPermissions(neededPermissions, PERMISSION_REQUEST);
                        }
                    }
                });
            }
        });

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.rgb(16, 24, 39));

        Button back = new Button(this);
        back.setText("← ফিরে যান");
        back.setOnClickListener(v -> {
            if (webView != null) {
                webView.stopLoading();
                webView.destroy();
                webView = null;
            }
            showHome();
        });
        layout.addView(back, new LinearLayout.LayoutParams(-1, dp(48)));
        layout.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(layout);
        webView.loadUrl(url);
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST && pendingRequest != null) {
            boolean granted = grantResults.length > 0;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) granted = false;
            }
            if (granted) {
                pendingRequest.grant(pendingRequest.getResources());
            } else {
                pendingRequest.deny();
                Toast.makeText(this, "অডিও/ভিডিও কলের জন্য অনুমতি প্রয়োজন", Toast.LENGTH_LONG).show();
            }
            pendingRequest = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
            showHome();
        } else {
            super.onBackPressed();
        }
    }
}
