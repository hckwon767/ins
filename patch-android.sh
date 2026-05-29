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

# 4. Java 파일 대상 디렉터리 찾기
JAVA_DIR=$(find android/app/src/main/java -type d -name "player" 2>/dev/null | head -1)
if [ -z "$JAVA_DIR" ]; then
  JAVA_DIR=$(find android/app/src/main/java -type d | sort | tail -1)
fi
echo "  Java dir: $JAVA_DIR"

# 5. MainActivity.java
cp android-patch/MainActivity.java "$JAVA_DIR/MainActivity.java"
echo "  MainActivity.java"

# 6. RadioForegroundService.java
cp android-patch/RadioForegroundService.java "$JAVA_DIR/RadioForegroundService.java"
echo "  RadioForegroundService.java"

# 7. media 의존성 확인 (build.gradle 에 media 없으면 추가)
GRADLE="android/app/build.gradle"
if ! grep -q "androidx.media:media" "$GRADLE"; then
  sed -i "/dependencies {/a\\    implementation 'androidx.media:media:1.7.0'" "$GRADLE"
  echo "  build.gradle: androidx.media 추가"
fi

echo ""
echo "All patches applied."
