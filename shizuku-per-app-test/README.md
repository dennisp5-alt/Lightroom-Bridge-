# Dennis Performance Manager — Shizuku Camera & Lightroom Trial v0.1.0

**Purpose:** An honest, reversible test of a **background device-idle exemption** on the Samsung stock Camera and Lightroom Mobile. It does not speed up or alter CPU, GPU, ISP, camera rendering, Lightroom processing, or image quality.

- Targets: `com.sec.android.app.camera` and `com.adobe.lrmobile` only.
- Native offline Java Android app. No network permission, root, analytics, external storage, or accessibility permissions.
- Shizuku v13 permission request and connection status.
- Inspect app status / standby bucket and compare elapsed task times.
- Apply `cmd deviceidle whitelist +PACKAGE` **only** after reading that the package is not already exempt; record an on-device restoration marker before modifying.
- Restore using `cmd deviceidle whitelist -PACKAGE` **only** when this trial added the exemption.
- All shell commands use a hardcoded allowlist; no arbitrary command runner.
- Shizuku shell access varies across Samsung/Android versions; errors and no-ops are reported.
- Camera is commonly already optimised. A non-change is a useful result.
- Uses Shizuku UserService over AIDL; commands are allowlisted in the privileged service.
- No automatic tracking of camera frames/Lightroom performance. Time the same task under comparable conditions; enter seconds manually.
- Before uninstalling the app, **restore** any active trial exemptions. Uninstalling does not reliably revert external system settings.

## Build

Run `gradle :app:assembleDebug` from this directory with JDK 17 and Android SDK 35. AGP 8.9.2. Build workflow: `.github/workflows/shizuku-performance-trial.yml` on `experiment/shizuku-camera-lightroom-v010`.

## Install / use

1. Install [Shizuku](https://shizuku.rikka.app/) and start its server using Android Wireless debugging.
2. Install the APK, launch **Dennis Performance Trial**, and grant Shizuku access.
3. Inspect both apps before changes. Record baseline task timing on the actual phone.
4. Enable only the single desired background trial, repeat a consistent task, record seconds.
5. Restore the original setting in the test app. Inspect again to confirm.

Do **not** claim that any faster foreground photo capture or export came from a background exemption without repeated A/B trials. This build intentionally does not attempt undocumented vendor performance controls, global throttling changes, force-stop, app-ops, or root access.
