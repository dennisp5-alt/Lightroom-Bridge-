# Dennis Lightroom Bridge — Android v0.1.0

**Development build:** Offline-first Android companion that analyses photos and generates individual Adobe Lightroom-compatible XMP develop presets. This is an initial source release. **An APK build and Lightroom Mobile import still require verification.**

## Features
- Select individual or multiple JPEG/HEIC photos through Android's system document picker.
- Offline image analysis: luminance percentiles, clipped highlights, dark regions, green dominance.
- Create a per-photo XMP preset with constrained tone, colour, sharpening and noise-reduction settings.
- Save one XMP preset or export presets for all selected photos into a chosen folder.
- Navigate selected photos and inspect proposed numerical Lightroom settings.
- Dark interface and project icon; app ID: `com.dennis.lightroombridge`.
- No internet permission, Adobe credentials, surveillance or global storage access.

## Build and install
1. Open the [Android APK workflow](https://github.com/dennisp5-alt/Lightroom-Bridge-/actions/workflows/android.yml).
2. Use **Run workflow** if an APK has not been built automatically.
3. Open the successful run and download artifact **Dennis-Lightroom-Bridge-v0.1.0-debug-apk**.
4. Extract the artifact ZIP and install `app-debug.apk` on Android.
5. If Actions reports an exhausted quota or storage restriction, use Android Studio with JDK 17, AGP 8.9.2, Gradle 8.11.1, SDK 35 and run `gradle :app:assembleDebug`.

**Artifact retention:** two days to reduce GitHub Actions storage usage. APK is debug-signed for testing, not a Play Store release. If a build fails, review the run logs before trying to install anything.

## Use
1. Tap **Select Photos**. Choose one or more local photos.
2. Review the per-photo suggested adjustments.
3. Tap **Save This Photo's XMP Preset** or **Export XMP Presets for All Selected Photos**.
4. In Lightroom Mobile select a photograph, go to Presets → Yours → menu → Import Presets, and select the saved `.xmp` file.
5. Apply it and compare before/after. Lightroom controls the rendered appearance and final full-resolution export.

XMP settings may overwrite **matching existing develop settings**, although the image original is untouched. Do not assume the preset can recover image data lost by phone JPEG processing.

## Scope and honesty
This v0.1 implementation is **histogram-based heuristic analysis**, not image-understanding AI, RAW processing or a live Lightroom render. It **does not** access Adobe's cloud albums, read private Lightroom databases, sync to Lightroom, or operate Lightroom sliders. Adobe's partner API permissions and safe cloud-write round-trip need separate development and testing.

No Adobe API credentials belong in source control, especially while this repository is public. See [docs/ROADMAP.md](docs/ROADMAP.md).

## Quality testing
The pure Kotlin engine has been compiled and run separately with a test JPEG, and the XMP parsed as XML. The Android app itself still needs CI compilation and device testing. Use a disposable photo for the first Lightroom import.
