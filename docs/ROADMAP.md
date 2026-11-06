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
