# 워치 나침반 — 휴대폰 목적지 안내

Wear OS 워치에서 휴대폰이 보낸 WGS84 위경도 좌표까지의 직선거리와 방향을 표시합니다. 목적지가 도착하기 전에는 `위치정보 없음`을 보여주며 GPS와 방향 센서를 사용하지 않습니다.

## 동작

휴대폰 앱이 Data Layer의 `/destination` 항목에 `name`(문자열), `latitude`(실수), `longitude`(실수)를 기록하면 워치가 수신합니다. 워치는 목적지 이름과 위경도를 간략히 알린 뒤 위치 권한을 확인하고 안내를 시작합니다. 목표 항목이 삭제되면 안내와 센서 사용도 멈춥니다. 앱이 다시 열릴 때 마지막으로 전달된 목적지도 불러옵니다.

거리와 방향은 워치의 최신 위치 측정으로 계산합니다. Android Fused Location Provider에 고정밀 업데이트를 요청하며, 워치의 GPS 및 시스템에서 사용할 수 있는 위치 소스를 이용합니다. 신선한 위치 측정이 있을 때만 거리 파동과 방향 화살표가 동작합니다. 추정 반경은 Android가 보고한 약 68% 신뢰 수준의 수평 정확도이며 최대 오차 보장은 아닙니다. 주소·도로 경로 거리와 고도 차이는 계산하지 않습니다.

화살표는 화면 정중앙에서 목표 방향을 가리키며, 희미한 동일 화살표가 수평계 역할을 합니다. 워치가 수평이면 두 화살표가 겹칩니다. 앱 화면을 벗어나거나 목표가 삭제되면 위치와 방향 센서를 중지합니다. 백그라운드 추적, 위치 기록 저장, 서버 전송은 하지 않습니다.

## 거리 파동 7단계

첨부 파동 컨셉처럼 중심에서 바깥으로 동심원이 퍼집니다. 멀 때는 어두운 파랑, 가까워지면 청록·금색·분홍·빨강으로 변하며 파동 수·속도·밝기가 증가합니다. `WaveStage.kt`의 `waveStages` 표에서 거리 하한, 색, 파동 수, 확산 주기, 밝기를 수정할 수 있습니다.

| 단계 | 거리 기준 | 색 | 확산 주기 |
|---|---|---|---|
| 1 | 500m 이상 | 희미한 파랑 | 7초 |
| 2 | 200~500m 미만 | 파랑 | 5.8초 |
| 3 | 50~200m 미만 | 밝은 파랑 | 4.6초 |
| 4 | 10~50m 미만 | 청록 | 3.4초 |
| 5 | 3~10m 미만 | 금색 | 2.5초 |
| 6 | 1~3m 미만 | 분홍 | 1.9초 |
| 7 | 0~1m 미만 | 빨강 | 1.4초 |

단계는 측정 거리와 GPS 추정 반경을 더한 보수적 거리로 정합니다. 거리 표시는 원래 측정 거리입니다. 예를 들어 거리 0.5m, 추정 반경 12m이면 4단계입니다. 7단계도 도착을 보장하지 않습니다. 위치가 없거나 10초 넘게 갱신되지 않으면 파동과 안내 화살표를 멈춥니다.

## 빌드

Android Studio에서 이 폴더를 열고 Gradle 동기화 후 실행합니다. `local.properties`의 `sdk.dir`은 버전 관리에서 제외합니다.

```sh
export JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## 워치 설치

맥과 워치를 같은 Wi-Fi에 연결하고 워치의 무선 디버깅 화면을 켭니다. 최초 페어링 코드 입력 후 연결 포트로 연결합니다. 페어링 포트와 연결 포트는 서로 다르며 무선 디버깅을 다시 켜면 바뀔 수 있습니다.

```sh
adb pair <워치IP>:<페어링포트>
adb connect <워치IP>:<연결포트>
adb devices -l
adb -s <워치IP>:<연결포트> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <워치IP>:<연결포트> shell am start -n com.nadaworks.watchnavigation/.MainActivity
```

`No route to host`가 표시되면 워치 Wi-Fi/화면 켜짐, 현재 IP·포트, 게스트 Wi-Fi의 기기 격리 및 macOS 로컬 네트워크 권한을 확인합니다.

## 기기 확인

1. 목적지를 보내기 전 `위치정보 없음`이 표시되고, 좌표 수신 후 이름·위도·경도 안내가 나타나는지 확인합니다.
2. 위치 권한을 허용하고 야외에서 워치 위치가 수신되면 거리와 추정 반경, 방향 및 파동이 표시되는지 확인합니다. 실내에서는 GPS가 오래 걸리거나 실패할 수 있습니다.
3. 휴대폰에서 새 좌표를 보내면 거리·방향·파동 단계가 갱신되고, 목적지 항목을 삭제하면 다시 대기 화면으로 돌아오는지 확인합니다.
4. 워치를 수평으로 들면 희미한 화살표가 중앙 화살표와 겹치는지 확인합니다.

패키지: `com.nadaworks.watchnavigation` · 앱 이름: `워치 나침반`

공식 참고: [Android Location](https://developer.android.com/reference/android/location/Location), [Compose for Wear OS](https://developer.android.com/training/wearables/compose), [Wi-Fi 디버깅](https://developer.android.com/training/wearables/get-started/debug-wifi).
