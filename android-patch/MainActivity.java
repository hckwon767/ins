package com.instation.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Foreground Service 시작 (앱 실행 즉시)
        startRadioService(RadioForegroundService.ACTION_START, "INSTATION", "", "");

        // 배터리 최적화 예외 요청 (삼성/샤오미 등 대응)
        requestBatteryOptimizationExemption();

        // JS → Java 브릿지 등록 (NowPlaying 정보 → 알림 업데이트)
        getBridge().getWebView().addJavascriptInterface(new RadioBridge(), "RadioBridge");
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
        // webView.onPause() 호출하지 않음 → 백그라운드에서 오디오 유지
        super.onPause();
    }

    @Override
    public void onStop() {
        // webView.pauseTimers() 호출하지 않음 → JS 계속 실행
        super.onStop();
    }

    @Override
    public void onDestroy() {
        startRadioService(RadioForegroundService.ACTION_STOP, "", "", "");
        super.onDestroy();
    }

    // ── Foreground Service 제어 ──────────────────────────────
    private void startRadioService(String action, String title, String artist, String channel) {
        Intent intent = new Intent(this, RadioForegroundService.class);
        intent.setAction(action);
        intent.putExtra(RadioForegroundService.EXTRA_TITLE,   title);
        intent.putExtra(RadioForegroundService.EXTRA_ARTIST,  artist);
        intent.putExtra(RadioForegroundService.EXTRA_CHANNEL, channel);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // ── 배터리 최적화 예외 요청 ───────────────────────────────
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    Intent intent = new Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                } catch (Exception e) {
                    // 일부 기기에서 해당 화면이 없는 경우 무시
                }
            }
        }
    }

    // ── JS → Native 브릿지 ────────────────────────────────────
    // JS에서 RadioBridge.updateNowPlaying(title, artist, channel) 호출 시
    // Foreground Service 알림을 업데이트함
    public class RadioBridge {
        @JavascriptInterface
        public void updateNowPlaying(String title, String artist, String channel) {
            runOnUiThread(() ->
                startRadioService(RadioForegroundService.ACTION_UPDATE, title, artist, channel)
            );
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(() ->
                startRadioService(RadioForegroundService.ACTION_STOP, "", "", "")
            );
        }
    }
}
