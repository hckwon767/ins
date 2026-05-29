#!/usr/bin/env bash
# ============================================================
#  patch-android.sh
#  cap sync 이후 실행: android/ 디렉터리에 패치 파일을 복사하고
#  build.gradle 수정을 적용합니다.
#  GitHub Actions 워크플로우에서 자동으로 호출됩니다.
# ============================================================
set -euo pipefail

ANDROID_APP="android/app/src/main"
ANDROID_RES="$ANDROID_APP/res"

echo "📦 Applying Android patches..."

# ── 1. AndroidManifest.xml ──────────────────────────────────
cp android-patch/AndroidManifest.xml "$ANDROID_APP/AndroidManifest.xml"
echo "  ✅ AndroidManifest.xml"

# ── 2. styles.xml (다크 테마) ───────────────────────────────
mkdir -p "$ANDROID_RES/values"
cp android-patch/styles.xml "$ANDROID_RES/values/styles.xml"
echo "  ✅ styles.xml"

# ── 3. network_security_config.xml ─────────────────────────
mkdir -p "$ANDROID_RES/xml"
cp android-patch/network_security_config.xml "$ANDROID_RES/xml/network_security_config.xml"
echo "  ✅ network_security_config.xml"

# ── 4. android:networkSecurityConfig 속성 주입 ──────────────
# Capacitor가 생성한 AndroidManifest의 <application> 태그에 이미 포함했으므로 skip.
# (android-patch/AndroidManifest.xml 에서 직접 관리)

# ── 5. build.gradle — minSdk / targetSdk / compileSdk 확인 ──
GRADLE="android/app/build.gradle"
# minSdk 를 26 으로 강제 (Capacitor 기본 22 → ExoPlayer 권장)
sed -i 's/minSdkVersion [0-9]*/minSdkVersion 26/' "$GRADLE"
# targetSdk 34
sed -i 's/targetSdkVersion [0-9]*/targetSdkVersion 34/' "$GRADLE"
echo "  ✅ build.gradle (minSdk=26, targetSdk=34)"

# ── 6. WebView 하드웨어 가속 확인 (MainActivity.java/.kt) ───
MAIN_JAVA=$(find android/app/src/main/java -name "MainActivity*" | head -1)
if [[ -f "$MAIN_JAVA" ]]; then
    # 이미 hardwareAccelerated="true" 는 Manifest에 선언되어 있으므로 OK
    echo "  ✅ MainActivity found: $MAIN_JAVA"
fi

# ── 7. proguard-rules.pro — Capacitor / 스트리밍 관련 keep ──
cat >> android/app/proguard-rules.pro << 'PROGUARD'

# Capacitor
-keep class com.getcapacitor.** { *; }
-keep @com.getcapacitor.annotation.CapacitorPlugin class * { *; }

# Background Mode 플러그인
-keep class de.appplant.cordova.plugin.background.** { *; }

# OkHttp / 스트리밍
-dontwarn okhttp3.**
-dontwarn okio.**
PROGUARD
echo "  ✅ proguard-rules.pro"

echo ""
echo "✅ All patches applied successfully."
