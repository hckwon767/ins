package com.instation.player;

import android.os.Bundle;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        WebView webView = getBridge().getWebView();
        if (webView != null) {
            webView.onResume();
            webView.resumeTimers();
        }
    }

    @Override
    public void onPause() {
        // webView.onPause() 호출하지 않음 -> 백그라운드에서 오디오 유지
        super.onPause();
    }

    @Override
    public void onStop() {
        // webView.pauseTimers() 호출하지 않음 -> JS 계속 실행
        super.onStop();
    }
}
