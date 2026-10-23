package com.swapbar.android;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

abstract class ClaudeSessionActivity extends Activity {
    abstract String dataSuffix();

    private static String configuredDataSuffix;
    private WebView webView;
    private String service;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (Build.VERSION.SDK_INT < 28) {
            Toast.makeText(this, "Claude oturumlarini ayirmak icin Android 9 veya yenisi gerekli.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // WebView's data suffix is process-global and can only be set once per process.
        // Android may keep this account process alive after the Activity is closed.
        String requestedSuffix = dataSuffix();
        if (configuredDataSuffix == null) {
            WebView.setDataDirectorySuffix(requestedSuffix);
            configuredDataSuffix = requestedSuffix;
        } else if (!configuredDataSuffix.equals(requestedSuffix)) {
            Toast.makeText(this, "Bu WebView sureci baska bir hesap oturumuna ayrilmis.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        boolean darkTheme = getIntent().getBooleanExtra("theme_dark", false);
        setTheme(darkTheme ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        int chrome = darkTheme ? Color.rgb(23, 23, 29) : Color.rgb(250, 249, 247);
        getWindow().setStatusBarColor(chrome);
        getWindow().setNavigationBarColor(chrome);
        getWindow().getDecorView().setSystemUiVisibility(darkTheme ? 0
                : android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        service = getIntent().getStringExtra("account_service");
        if (!"ChatGPT".equals(service)) service = "Claude";

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(chrome);
        page.setPadding(0, dp(8), 0, 0);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(12), 0, dp(12), dp(8));
        Button backToAccounts = new Button(this);
        backToAccounts.setText("\u2190 Hesaplar");
        backToAccounts.setAllCaps(false);
        backToAccounts.setTextSize(12);
        bar.addView(backToAccounts, new LinearLayout.LayoutParams(-2, dp(44)));
        backToAccounts.setOnClickListener(v -> finish());
        TextView title = new TextView(this);
        title.setText(service + "  ·  " + getIntent().getStringExtra("account_label"));
        title.setTextColor(darkTheme ? Color.rgb(240, 238, 247) : Color.rgb(45, 42, 39));
        title.setTextSize(16);
        title.setMaxLines(1);
        title.setPadding(dp(8), 0, dp(6), 0);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        Button paste = new Button(this);
        paste.setText("Giris baglantisini yapistir");
        paste.setAllCaps(false);
        paste.setTextSize(12);
        if ("ChatGPT".equals(service)) paste.setVisibility(android.view.View.GONE);
        bar.addView(paste, new LinearLayout.LayoutParams(-2, dp(44)));
        paste.setOnClickListener(v -> pasteLoginLink());
        page.addView(bar);

        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(Uri.parse(url));
            }
        });
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setBackgroundColor(chrome);
        page.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView hint = new TextView(this);
        hint.setText("Her kartin oturumu ayri saklanir. Google/Apple/Microsoft girisi desteklenmez.");
        hint.setTextColor(darkTheme ? Color.rgb(169, 167, 181) : Color.rgb(100, 97, 91));
        hint.setTextSize(11);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(12), dp(8), dp(12), dp(10));
        page.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        setContentView(page);
        if (state == null) webView.loadUrl(homeUrl());
        else webView.restoreState(state);
    }

    private boolean route(Uri uri) {
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (isExternalIdentityProvider(host)) {
            Toast.makeText(this, "Google, Apple veya Microsoft girisi bu izole WebView'de desteklenmiyor. E-posta girisini dene.", Toast.LENGTH_LONG).show();
            return true;
        }
        if (isServiceDomain(host)) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception ignored) {
            Toast.makeText(this, "Baglanti acilamadi.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    private String homeUrl() {
        return "ChatGPT".equals(service) ? "https://chatgpt.com/" : "https://claude.ai/";
    }

    private boolean isServiceDomain(String host) {
        if ("ChatGPT".equals(service)) {
            return host.equals("chatgpt.com") || host.endsWith(".chatgpt.com")
                    || host.equals("openai.com") || host.endsWith(".openai.com");
        }
        return host.equals("claude.ai") || host.endsWith(".claude.ai")
                || host.equals("anthropic.com") || host.endsWith(".anthropic.com");
    }

    private boolean isExternalIdentityProvider(String host) {
        return host.equals("accounts.google.com") || host.endsWith(".accounts.google.com")
                || host.equals("appleid.apple.com") || host.endsWith(".appleid.apple.com")
                || host.equals("login.live.com") || host.endsWith(".login.live.com")
                || host.equals("login.microsoftonline.com") || host.endsWith(".login.microsoftonline.com");
    }

    private void pasteLoginLink() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = clipboard == null ? null : clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            Toast.makeText(this, "Once e-postadaki giris baglantisini kopyala.", Toast.LENGTH_LONG).show();
            return;
        }
        String copied = String.valueOf(clip.getItemAt(0).coerceToText(this)).trim();
        Uri uri = Uri.parse(copied);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !isServiceDomain(uri.getHost() == null ? "" : uri.getHost().toLowerCase())) {
            Toast.makeText(this, "Panoda bu servis icin gecerli bir HTTPS giris baglantisi yok.", Toast.LENGTH_LONG).show();
            return;
        }
        webView.loadUrl(uri.toString());
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        if (webView != null) webView.saveState(out);
        super.onSaveInstanceState(out);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            if (webView.getParent() instanceof ViewGroup) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
