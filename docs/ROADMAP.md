# Dennis Lightroom Bridge engineering roadmap

## v0.1 — offline Android baseline
- Local photo picker with explicit user consent.
- JPEG/HEIC preview and histogram-derived adjustments.
- Validated XML/XMP generation.
- Android-selected directory for batch XMP output.
- Not yet a remotely connected plugin.

**Acceptance tests:** CI compiles the Android project; launch on Samsung A54/S23 Ultra; import individual XMP into Lightroom Mobile; examine tone, clarity and colour; verify originals untouched.

## v0.2 — quality and reliability
- Correct orientation and image colour space handling.
- Non-blocking batch progress, cancel and per-image errors.
- Detect prior edits when possible; preserve adjustable Lightroom settings.
- Better night, portraits and skies; high-ISO noise estimates; semantic scene analysis only if supported.
- Validate XMP against current Lightroom Mobile imports.

## v0.3 — ChatGPT connection
- Private, authenticated HTTPS bridge/MCP server with explicit photo transfer from Android, consent and revocation.
- Tools such as list_selected_photos, analyse_photo and generate_xmp.
- No claim of automatic Android local-file access from a ChatGPT skills-only plugin.
- Private credential and photo retention policies.

## v0.4 — Adobe cloud API
- Apply for Lightroom Partner API access; implement Adobe OAuth, least privileges, album browsing and read-only photo/thumbnail retrieval.
- Never access Lightroom's Android sandbox or copy authentication tokens from the app.
- No credential values in GitHub.

## v0.5 — cloud edit round trip
- On a disposable cloud photo only, test develop XMP API behaviour and sync consistency.
- Confirm Adobe approval, permitted write scopes, previous-edit backup and rollback.
- Expose remote write only after explicit approval and verified recovery.

## Important constraints
This is not a Lightroom clone. JPEG correction cannot reconstruct blown pixels or baked-in sharpening halos. An XMP import is not proof of automated adjustment of existing Lightroom Mobile images. Original files are never changed by the Android v0.1 implementation.

## v0.1.2 — explanation-first editing
- Human-readable purpose-based edit labels and explanations.
- Copy a slider checklist for a manual Lightroom edit without importing another preset.
- Optional XMP with editable descriptive name and explicit warning for bulk export.
- Direct Lightroom UI control is still unavailable; future Android accessibility experiments require consent, calibration, UI version checks and no blanket access to the device.

## Feasibility of direct control
Adobe's Lightroom Partner API requires partner entitlement and user OAuth and does not inherently grant unrestricted direct slider control on an Android phone. A separate experimental Android AccessibilityService might perform gestures on visible Lightroom controls, subject to user permission and exact UI recognition; never run hidden/unsupervised.
