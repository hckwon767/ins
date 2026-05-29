# INSTATION — Capacitor Android 빌드 가이드

`instation-player.html` 한 파일을 그대로 Capacitor로 감싸서 Android APK를 자동 빌드합니다.

---

## 폴더 구조

```
instation-capacitor/
├── www/
│   └── index.html              ← instation-player.html (그대로 복사)
├── android-patch/
│   ├── AndroidManifest.xml     ← 권한/서비스 선언 패치
│   ├── styles.xml              ← 다크 테마 (#000 배경)
│   └── network_security_config.xml  ← 스트리밍 서버 HTTPS 허용
├── .github/
│   └── workflows/
│       └── build-android.yml  ← GitHub Actions CI/CD
├── capacitor.config.json
├── package.json
├── patch-android.sh            ← 패치 자동 적용 스크립트
└── README.md
```

---

## 로컬 첫 세팅 (최초 1회)

```bash
# 1. 의존성 설치
npm install

# 2. Android 플랫폼 추가
npx cap add android

# 3. www → WebView 동기화
npx cap sync android

# 4. 패치 적용
chmod +x patch-android.sh && ./patch-android.sh

# 5. Android Studio로 열기 (확인용)
npx cap open android
```

---

## GitHub Actions로 APK 자동 빌드

### 저장소 준비

```bash
git init
git remote add origin https://github.com/<your-org>/instation.git
git add .
git commit -m "init: Capacitor + GitHub Actions"
git push -u origin main
```

push 만 하면 **debug APK** 가 자동으로 빌드됩니다.  
Actions 탭 → 워크플로우 실행 → Artifacts 에서 다운로드.

---

## Release APK 빌드 (서명 포함)

### 1단계: 키스토어 생성 (최초 1회, 로컬에서)

```bash
keytool -genkey -v \
  -keystore instation.keystore \
  -alias instation \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

> ⚠️ `instation.keystore` 파일은 **절대 git에 커밋하지 마세요**.

### 2단계: 키스토어를 Base64로 인코딩

```bash
# macOS / Linux
base64 -i instation.keystore | pbcopy    # 클립보드에 복사 (macOS)
base64 -i instation.keystore             # 출력 복사 (Linux)

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes('instation.keystore')) | clip
```

### 3단계: GitHub Secrets 등록

GitHub 저장소 → **Settings → Secrets and variables → Actions → New repository secret**

| Secret 이름              | 값                              |
|--------------------------|----------------------------------|
| `KEYSTORE_BASE64`        | 위에서 복사한 Base64 문자열      |
| `KEYSTORE_PASSWORD`      | 키스토어 비밀번호                |
| `KEYSTORE_ALIAS`         | `instation`                      |
| `KEYSTORE_ALIAS_PASSWORD`| 키 비밀번호 (키스토어 비밀번호와 같아도 됨) |

### 4단계: 태그를 push하면 자동 릴리즈

```bash
git tag v1.0.0
git push origin v1.0.0
```

→ GitHub Actions가 Release APK를 빌드하고 **GitHub Releases** 페이지에 자동 첨부합니다.

---

## 수동 빌드 (Actions 탭에서)

1. GitHub 저장소 → **Actions** 탭
2. `Build INSTATION Android APK` 워크플로우 선택
3. **Run workflow** → `build_type` 선택 (`debug` 또는 `release`)

---

## 빌드 흐름 요약

```
git push / tag push
       ↓
GitHub Actions Runner (ubuntu-latest)
       ↓
npm ci  →  npx cap add android (최초)
       ↓
npx cap sync android      ← www/index.html → WebView 자동 복사
       ↓
./patch-android.sh         ← Manifest, 테마, 네트워크 보안 패치
       ↓
./gradlew assembleDebug    ← debug 빌드
또는
./gradlew assembleRelease  ← release 빌드 (키스토어 서명)
       ↓
Artifacts 업로드 / GitHub Release 생성
```

---

## 플러그인 설명

| 플러그인 | 역할 |
|---|---|
| `@capacitor/status-bar` | 상태바 검정 배경 (#000) |
| `@capacitor/splash-screen` | 스플래시 1.5초 표시 |
| `@capacitor-community/background-mode` | 화면 꺼짐 후 오디오 스트리밍 유지 |

---

## www/index.html 수정 시

```bash
# HTML 수정 후
npx cap sync android

# 또는 그냥 git push → GitHub Actions가 자동으로 재빌드
```

---

## 자주 묻는 문제

**Q: `cap sync` 후 앱을 설치했는데 오디오가 백그라운드에서 끊겨요.**  
A: `@capacitor-community/background-mode` 플러그인이 설치되어 있는지 확인하고, 기기 배터리 최적화 설정에서 INSTATION을 제외하세요.

**Q: 스트리밍 서버 연결 오류가 나요.**  
A: `network_security_config.xml` 이 적용됐는지 확인하세요. `patch-android.sh` 를 다시 실행해보세요.

**Q: 릴리즈 빌드 서명 오류가 나요.**  
A: Secrets 이름 철자를 다시 확인하세요. Base64 인코딩 시 개행 문자가 포함되지 않았는지 확인하세요 (`base64 -w 0` 옵션 사용).
