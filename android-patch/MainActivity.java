package com.instation.player;

import android.os.Bundle;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 포그라운드 복귀 시 WebView 재개
        WebView webView = getBridge().getWebView();
        if (webView != null) webView.onResume();
        webView.resumeTimers();
    }

    @Override
    protected void onPause() {
        // 백그라운드 진입 시 WebView를 멈추지 않음 (오디오 유지)
        // super.onPause()는 호출하되 webView.onPause()는 호출 안 함
        super.onPause();
    }

    @Override
    protected void onStop() {
        // WebView 타이머 유지 (JS setInterval 등 계속 실행)
        // webView.pauseTimers() 호출 안 함
        super.onStop();
    }
}
