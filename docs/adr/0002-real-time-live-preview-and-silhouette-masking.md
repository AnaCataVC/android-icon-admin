# ADR 0002: Real-time Live Preview, Silhouette Masking, and Preset Persistence

## Context
In `AndroidIconsAdmin`, the Style Studio screen (`StyleStudioScreen`) allows users to customize container shapes, background colors, padding margins, treatments (saturation, brightness, contrast), and layer extraction modes.

Previously, changes to presets, shapes, and colors did not update the "Vista Previa en Vivo" (Live Preview) because:
1. Local slider/chip state was only consulted during the "Guardar" action, while the preview row was bound to `MainViewModel.apps` (which only refreshed upon explicit batch updates).
2. Saving any style persisted a single file to `StyleStore`, causing subsequent calls to `getAll()` to omit the remaining standard presets.
3. The alpha compositing logic in `IconStyleRenderer.composite` did not strictly clip foreground pixels against the container shape silhouette, allowing full-bleed icons to leak into rectangular corners.

## Decision
1. **Reactive Live Preview**: `StyleStudioScreen` derives a reactive `currentDraftStyle` and asynchronously generates preview bitmaps using `produceState` on `Dispatchers.Default`. If no apps are installed/discovered or snapshots are pending, deterministic geometric fallback icons (Phone, Messages, Camera, Music, Settings) ensure the preview remains functional and responsive.
2. **Preset Preservation**: `StyleStore.getAll()` guarantees that all standard presets (`IconStyle.getStandardPresets()`) remain accessible, merging in-place with user overrides or custom saved styles.
3. **Silhouette Masking**: `IconStyleRenderer.composite` masks the composite alpha strictly by the container shape's alpha coverage (`ba`), discarding pixels outside the boundary (`ba <= 0.0`) and preserving anti-aliased edge falloff.
4. **Multi-Mode Snapshot Storage**: `OriginalSnapshotStore` and `MainViewModel.refreshPreviews` cache snapshots by extraction mode (`SMART_FOREGROUND`, `MONOCHROME_PREFERRED`, etc.) with fallback to the base snapshot.

## Consequences
- **Positive**: Instant visual feedback in Style Studio when switching presets, adjusting shapes, changing colors, or dragging sliders.
- **Positive**: Standard presets never disappear from the UI after saving.
- **Positive**: Strict silhouette masking ensures geometric shapes (circles, squircles, teardrops) are always cleanly defined without foreground bleeding.
- **Positive**: Backwards-compatible unit tests in `:core` verify preset preservation, strict container masking, and multi-mode snapshot retrieval.
