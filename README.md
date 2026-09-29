# 워치 나침반 — GPS 목표 안내

초기 테스트 목표는 **위도 37.53614, 경도 127.13265 (WGS84)** 입니다.
워치의 GPS 위치로 목표까지의 지표면 직선거리(m)와 방향을 갱신합니다.
주소·도로 경로 거리나 고도 차이는 계산하지 않습니다. 테스트 좌표는 `Destination.kt`의 `testDestinations`에서 수정합니다.

- 상단: 목표까지 거리(m). GPS를 못 받으면 `— m`.
- 하단: `GPS 추정 반경 N m`, 측정 후 경과 초, 수평/센서 안내.
- 추정 반경은 Android가 보고한 약 68% 신뢰 수준의 수평 위치 정확도이며 최대 오차 보장이 아닙니다.
  실제 오차를 검증하려면 별도로 정확히 측량된 현재 위치가 필요합니다. 목표 핀의 오차도 포함되지 않습니다.
- GPS는 약 1초 간격으로 요청하나 수신 환경에 따라 실제 갱신 간격이 달라집니다.
- 10초 넘게 새 측정이 없으면 거리색을 흐리게 하고 `갱신 대기`를 표시합니다.
- 거리 ≤ 추정 반경일 때는 `목표 근처 · 방향 불확실`을 표시하고 방향 화살표를 흐리게 표시합니다.
  위치가 없거나 오래됐을 때도 화살표를 안내 방향으로 사용하지 않습니다.
- 화살표는 `Location.bearingTo()`의 진북 방위각과 자기 편각으로 보정한 센서 방향을 비교합니다.
- 기존 원형 꼬리와 수평계는 유지합니다. 4° 이내는 중심, 25°에서 최대 이동, 약 0.2초 필터를 적용합니다.
- 정확한 위치 권한을 앱 사용 중에만 요청합니다. 권한 거부·대략적 위치만 허용·GPS 꺼짐은 상태와 설정 진입을 제공합니다.
- 화면을 떠나면 GPS와 방향 센서를 중지합니다. 백그라운드 추적·서버 전송·위치 기록 저장은 없습니다.

## 테스트 좌표 버튼

앱을 열면 좌표1·좌표2·좌표3 선택 버튼을 표시합니다. 작은 워치에서는 스크롤할 수 있습니다.
선택하면 나침반으로 돌아오고, 상단 `좌표N · 변경`을 누르면 다시 선택할 수 있습니다.

| 버튼 | 위도 | 경도 | 기준 |
|---|---|---|---|
| 좌표1 | 37.53614 | 127.13265 | 사용자가 제공한 목표 |
| 좌표2 | 37.53714 | 127.13265 | 좌표1에서 북쪽 약 111m |
| 좌표3 | 37.53614 | 127.13385 | 좌표1에서 동쪽 약 106m |

버튼은 `LocationTracker.updateDestination(Destination(...))`을 호출합니다.
향후 휴대폰에서 받은 좌표도 이 함수에 전달하면 됩니다. 수신 전송 계층은 아직 없습니다.
목표 변경 시 최근 GPS 측정으로 즉시 재계산하되 측정 시각·정확도는 그대로 유지합니다.
GPS가 없으면 선택한 목표로 다음 측정을 기다리고, 오래된 GPS는 기존 갱신 대기 정책을 유지합니다.
잘못된 위경도는 `Destination` 생성 시 거부합니다. 테스트 선택은 메모리 상태이며 앱 재생성 시 좌표1로 돌아갑니다.

검증: GPS 수신 후 1→2→3→1을 선택해 거리와 방향이 바뀌고 최초 값으로 돌아오는지 확인합니다.
가만히 있어도 GPS 오차로 값은 소폭 변할 수 있습니다. GPS 수신 전과 오래된 상태에서도 목표 전환을 확인합니다.

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

1. 첫 실행 시 **정확한 위치 / 앱 사용 중에만 허용**을 선택합니다.
2. 야외에서 GPS 수신을 기다립니다. 실내에서는 수신이 오래 걸리거나 실패할 수 있습니다.
3. 실제 거리와 추정 반경이 표시되고, 이동하면 거리와 측정 시각이 갱신되는지 확인합니다.
4. 충전기·자석을 피하고 워치를 수평으로 들면 수평계 원이 중앙에 겹치는지 확인합니다.
5. 워치를 돌려도 꼬리 원은 화면 중심에 유지되고 화살표는 목표 지점을 가리키는지 확인합니다.
6. GPS를 끄면 안내가 중단되고 설정 진입이 보이는지, 다시 켜면 새 측정을 받는지 확인합니다.
7. 권한 거부 또는 대략적 위치만 허용해도 충돌 없이 정확한 위치 권한 안내를 표시해야 합니다.
8. 홈으로 나가면 GPS/센서 구독을 해제하며, 돌아오면 캐시 대신 새로운 측정을 기다립니다.
9. 목표 근처나 오래된 측정에서 도착 또는 방향 정확도를 확정하지 않는지 확인합니다.

## 다음 단계

휴대폰 지도에서 목표 선택 → Wearable Data Layer로 목적지 전달 순으로 추가합니다.

공식 참고: [Android Location](https://developer.android.com/reference/android/location/Location),
[자기 편각](https://developer.android.com/reference/android/hardware/GeomagneticField),
[Compose for Wear OS](https://developer.android.com/training/wearables/compose),
[Wi-Fi 디버깅](https://developer.android.com/training/wearables/get-started/debug-wifi).
