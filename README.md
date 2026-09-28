# 워치 나침반 — 1단계

워치 방향 센서를 읽고 자기 북쪽에서 시계방향 45°인 북동쪽을 가리키는 Wear OS 테스트 앱입니다.
현재 테스트 목표는 GPS 좌표가 아닌 고정 방위각이며, 화살표 회전각은 `45° - 현재 워치 방위각`입니다.
휴대폰 앱, GPS, 목적지 좌표, 네트워크는 아직 포함하지 않습니다.
화살표 꼬리 원을 화면 정중앙에 고정하고, 다른 색의 원으로 수평을 안내합니다.
수평계는 4° 이내에서 중앙에 붙고, 25°에서 최대 이동하며 약 0.2초 필터로 작은 흔들림을 완화합니다.
감도는 `LevelIndicator.kt`에서 조절합니다. 화살표 방위각에는 이 필터를 적용하지 않습니다.

## 개발 환경

- Kotlin (AGP 내장 2.2.10), Compose compiler 2.2.10
- Wear Compose Material 3 1.7.0, Compose BOM 2026.09.00
- Android Gradle Plugin 9.4.1, Gradle 9.6.0
- JDK 17 이상, Android SDK 37(컴파일), target API 36, 최소 API 30
- 패키지: `com.nadaworks.watchnavigation`, 앱 이름: `워치 나침반`

Android Studio에서 이 폴더를 열고 Gradle 동기화 후 실행합니다.
`local.properties`의 `sdk.dir`에는 본인 Android SDK 경로를 지정합니다(버전 관리 제외).

```sh
# 이 맥에 설치된 Android Studio의 JDK 사용
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 워치 연결과 설치

맥과 워치가 같은 Wi-Fi에 연결되어 있어야 합니다. 워치의 무선 디버깅 화면을 켜두세요.
최초 페어링 시 아래 명령을 실행하고 워치의 6자리 코드를 입력합니다.
페어링 포트와 연결 포트는 서로 다르며, 디버깅 재활성화 시 바뀔 수 있습니다.

```sh
adb pair <워치IP>:<페어링포트>
adb connect <워치IP>:<연결포트>
adb devices -l
adb -s <워치IP>:<연결포트> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <워치IP>:<연결포트> shell am start -n com.nadaworks.watchnavigation/.MainActivity
```

`No route to host`는 아직 기기에 네트워크 연결이 되지 않은 상태입니다.
워치 Wi-Fi/화면 켜짐, 현재 IP·포트, 게스트 Wi-Fi의 기기 격리 및 macOS 로컬 네트워크 권한을 확인합니다.

## 실기기 합격 조건

1. 앱 실행 후 원형 꼬리가 있는 윤곽 화살표가 보이며 큰 각도 숫자는 표시하지 않습니다.
   첫 이벤트 전에는 수평계 원을 숨기고 흐린 화살표와 대기 상태를 표시합니다.
2. 충전기·자석·금속 물체를 피하고 워치를 화면이 위로 향하게 평평하게 둡니다.
3. 워치를 돌리면 꼬리 원은 정중앙에 머물고 화살표는 반대 방향으로 돌아 같은 북동쪽을 가리킵니다.
   워치 방위각 0°에서는 오른쪽 위, 45°에서는 위, 90°에서는 왼쪽 위를 가리킵니다.
4. 359°↔0° 경계를 지나도 화살표 방향이 이어집니다(애니메이션 없이 센서값을 직접 반영).
5. 홈으로 나가면 센서 구독이 해제되고, 돌아오면 새 측정값을 기다립니다.
6. 센서 부재·등록 실패·낮은 정확도는 화면에 표시합니다.

7. 오른쪽·왼쪽·위·아래를 높이면 주황색 수평계 원이 높은 쪽으로 이동합니다.
8. 평평하게 들면 수평계 원이 꼬리 원 안에 동심원으로 겹치고 보라색과 `수평이 맞아요`로 표시됩니다.
9. 화면을 뒤집으면 수평으로 표시하지 않습니다. 수평 상태에서 방향만 돌리면 수평계는 중앙을 유지합니다.

수평계는 자세 안내용이며 정밀 측정기는 아닙니다. 실제 워치에서 네 방향 반응과 감도를 확인해야 합니다.
손목을 세운 자세에서는 방향이 불안정할 수 있습니다. 이번 단계는 평평한 자세의 센서 반응 검증이며,
진북 보정이나 목적지 안내 정확도를 검증한 버전은 아닙니다. AOD와 백그라운드 추적도 포함하지 않습니다.

## 다음 단계

고정 좌표의 목적지 방위각 → 워치 GPS와 거리 → 휴대폰 지도 → Wearable Data Layer 순서로 추가합니다.
GPS 방위각(진북)과 센서 방위각(자북)을 함께 쓰는 단계에서는 자기 편각 보정이 필요합니다.

공식 참고: [Compose for Wear OS](https://developer.android.com/training/wearables/compose),
[Wi-Fi 디버깅](https://developer.android.com/training/wearables/get-started/debug-wifi).
