# ADR 0001: Modular Cleanroom Architecture (:core JVM + :app Android)

## Context
`AndroidIconsAdmin` requires a high-performance, deterministic image processing pipeline that normalizes app icons, applies mathematical treatments (Rec.709 saturation, contrast curves, brightness offsets, and 15% tint blending), and renders subpixel anti-aliased container shapes (Squircle, One UI 8.5 Squircle, Rounded Square, Circle, Teardrop).

On Android, image manipulation often relies heavily on `android.graphics.Bitmap` and `android.graphics.Canvas`. However, coupling the domain engine to Android APIs makes fast, isolated unit testing difficult, requiring slow emulator runs or Robolectric instrumentation.

## Decision
We enforce a Cleanroom modular architecture dividing the codebase into two discrete components:
1. **`:core`**: A target-independent Kotlin 2.1 JVM library with zero `android.*` dependencies. It manages the `RgbaCanvas` byte buffer, bilinear `IconNormalizer`, `IconStyleRenderer`, presets, `RuleEngine` with ReDoS protection, `StyleStore`, `AppSettingsService`, and `AppIconUndoStore` with atomic file writes and `.bak` corruption recovery.
2. **`:app`**: The Android presentation and platform integration layer (`minSdk = 26`, `targetSdk = 35/36`, Jetpack Compose Material 3). It handles package discovery, `AdaptiveIconDrawable` layer extraction, Home Screen Widgets (`AppWidgetProvider`), silent bulk shortcut updates (`ShortcutManagerCompat`), and export routines (Samsung One UI Theme Park MediaStore albums & ZIP bundles).

## Consequences
- **Positive**: Core algorithms execute and verify in milliseconds on standard JVM JUnit 5 test runners (`./gradlew :core:test`).
- **Positive**: Zero coupling to OEM-specific launcher behaviors in the core domain logic.
- **Trade-off**: Requires lightweight byte array mapping (`BitmapCanvasBridge`) when passing bitmaps between the Android UI and the `:core` renderer.
