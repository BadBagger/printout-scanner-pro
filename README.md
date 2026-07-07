# Printout Scanner Pro

Native Android MVP by Smithware Studios.

Printout Scanner Pro turns workplace paper printouts into organized local trackers. The MVP focuses on training reports:

- scan or import a printout image
- run on-device OCR
- review and edit extracted rows before saving
- save confirmed trackers locally with Room
- prioritize training follow-up with Training Radar
- mark associates working today
- copy or share plain-text reports

Privacy: scanned printouts, extracted text, associates, due dates, and notes stay local on device for this MVP. No account, ads, tracking, or cloud upload is required.

## Build

```powershell
$env:JAVA_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.local-jdk\jdk-17.0.19+10'
$env:ANDROID_HOME='C:\Users\KyleB\Documents\Codex\2026-07-04\build-a-native-android-app-using\.android-sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :app:testDebugUnitTest :app:assembleRelease
```

