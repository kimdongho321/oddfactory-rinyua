# 리뉴아 극장 알바

트릭컬 리바이브의 리뉴아를 소재로 만든 비공식·비상업 팬 프로젝트입니다. Android 홈 화면에 작은 매표소를 띄우고, 선택한 앱을 검표 연출 뒤 실행합니다.

현재 소스는 **0.16.8 (버전 코드 24)** 기준입니다. 앱 사용 정보와 실행 실패 예외를 기록하던 진단 로그를 제거했습니다.

## 빌드

JDK 21과 Android SDK Platform 35를 준비하세요. 터미널에서 JDK를 `JAVA_HOME`으로, Android SDK를 `ANDROID_HOME`으로 지정합니다. 첫 빌드에서는 Gradle과 Android 빌드 도구를 내려받습니다.

- Windows PowerShell — `.\gradlew.bat :app:assembleDebug`
- macOS / Linux — `sh ./gradlew :app:assembleDebug`

디버그 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다. Android 11 이상이 필요합니다. 디버그 빌드와 배포 APK의 서명이 다르므로 Android에서 같은 앱의 업데이트로 설치할 수 없습니다.

## 폰에서 사용하기

앱을 열고 **매표소 실행**을 누릅니다. Android 설정에서 **리뉴아 캐릭터 표시** 접근성 서비스를 직접 켜고 돌아옵니다. 기본 홈 화면 전용 모드에서는 사용정보 접근을 허용합니다. 실행할 앱을 고르고, Android 13 이상에서는 알림 권한 안내에 응답합니다. 매표소를 한 번 누르면 앱 선택, 두 번 누르면 메뉴, 길게 누르면 위치 이동입니다.

앱은 폰에서 실행하고 다시 시작할 수 있도록 구성되어 있습니다. 접근성 서비스는 화면 내용 읽기나 제스처 대신 실행 없이 오버레이 표시와 입력 영역에 사용합니다. 사용정보 접근은 홈 화면 감지에 사용합니다. 업데이트 확인은 앱을 열거나 **업데이트 확인**을 누를 때 공개 버전 목록에 접속합니다. 매표소와 앱 실행은 오프라인에서도 사용할 수 있도록 구성되어 있습니다.

## 출처와 고지

트릭컬 리바이브와 리뉴아 캐릭터 IP는 에피드게임즈에 있습니다. 동봉한 도트 그림은 팬 제작 자산입니다. [에피드게임즈 2차 창작 가이드라인](https://epidgames.oqupie.com/portal/2399/article/73315)

이 앱에는 메이플스토리가 제공한 메이플스토리 서체가 적용되어 있습니다. [서체 사용 조건](app/src/main/assets/licenses/Maplestory-LICENSE.txt)

Gradle Wrapper의 [라이선스](gradle/LICENSE)와 [고지](gradle/NOTICE)를 함께 제공합니다.
