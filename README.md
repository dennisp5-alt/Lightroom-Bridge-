# Dennis Lightroom Bridge — Android v0.1.2

Offline Android companion for Lightroom Mobile editing assistance. v0.1.2 focuses on **descriptive edit names and a preset-free workflow**.

## Quick start — no preset clutter
1. Select one or more local JPEG/HEIC photos using Android's document picker.
2. Tap a photo to view its descriptive edit intent, e.g. **Natural Greens • Shadow Lift**, and a plain-language explanation.
3. Tap **COPY SETTINGS — NO PRESET CREATED**. This copies human-readable Lightroom target values to the Android clipboard.
4. Tap **OPEN LIGHTROOM**, open the *same* photo, and manually enter the displayed target values in Light, Colour/Mix, Effects and Detail. You can switch between the apps to inspect the values.
5. If the edit suits several photos, use Lightroom's official **Copy Settings / Paste Settings** command between them.

**Important:** Copy Settings in the Bridge copies *text*, not Lightroom's internal develop settings. It does **not** remotely apply changes. Lightroom's own Copy/Paste works after you have applied the first edit in Lightroom.

## Optional XMP
- **SAVE NAMED XMP PRESET** asks for an understandable preset name (default based on luminance and colour, not a camera filename).
- **ADVANCED: EXPORT ALL XMP PRESETS** requires confirmation, because importing many presets causes clutter.
- Do not import individual XMPs for every photo unless you explicitly want to keep them. Lightroom's own preset manager handles deleting, renaming and organising imported presets.
- XMP import is a manual Lightroom action; no Adobe cloud write or library access yet.

## Build
Open the [GitHub Actions APK workflow](https://github.com/dennisp5-alt/Lightroom-Bridge-/actions/workflows/android.yml), run it or use its latest successful automatic run. Download the `Dennis-Lightroom-Bridge-v0.1.2-debug-apk` artifact, extract its ZIP and install the APK.

Android: minimum 26; compileSdk 35; debug app ID `com.dennis.lightroombridge`; source and Java 17 build in this repository. APK is a testing build.

## Privacy and reliability
- No network permission, no Adobe passwords and no access to Lightroom's private database.
- No original JPEG/HEIC file is edited by the app.
- Histogram-derived edits are conservative **heuristics**, not subject detection or an actual Lightroom-rendered preview.
- Names represent measurable tone/green tendencies; they never pretend to recognise a landscape/portrait/sky.
- Applying imported XMP edits may replace corresponding current develop adjustments in Lightroom.
- Direct Lightroom slider gestures are **not implemented**. An optional Android AccessibilityService control prototype may be investigated later with explicit user consent and on-device calibration; it would be fragile across Lightroom UI updates and would not grant Adobe cloud-account access.
