#!/usr/bin/env bash
set -euo pipefail

ANDROID_APP="android/app/src/main"
ANDROID_RES="$ANDROID_APP/res"

echo "Applying Android patches..."

# 1. AndroidManifest.xml
cp android-patch/AndroidManifest.xml "$ANDROID_APP/AndroidManifest.xml"
echo "  AndroidManifest.xml"

# 2. styles.xml
mkdir -p "$ANDROID_RES/values"
cp android-patch/styles.xml "$ANDROID_RES/values/styles.xml"
echo "  styles.xml"

# 3. network_security_config.xml
mkdir -p "$ANDROID_RES/xml"
cp android-patch/network_security_config.xml "$ANDROID_RES/xml/network_security_config.xml"
echo "  network_security_config.xml"

# 4. MainActivity.java (WebView 백그라운드 일시정지 방지)
MAIN_DIR=$(find android/app/src/main/java -type d -name "player" 2>/dev/null | head -1)
if [ -z "$MAIN_DIR" ]; then
  MAIN_DIR=$(find android/app/src/main/java -type d | tail -1)
fi
cp android-patch/MainActivity.java "$MAIN_DIR/MainActivity.java"
echo "  MainActivity.java -> $MAIN_DIR"

echo ""
echo "All patches applied."
