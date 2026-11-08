> **Created:** 2026-11-07
> **Last Updated:** 2026-11-07

# Android Icons, Adaptive Icon Pipeline, and Launcher Integration Research

## 1. Visual Pipeline Port from `windows-icons-admin`

The reference desktop project (`windows-icons-admin`) implements a deterministic, zero-AI, offline-first visual styling pipeline in `WindowsIconsAdmin.Core.Styling`. Below is the breakdown of its mathematical and architectural primitives and how they map directly to `AndroidIconsAdmin`.

### 1.1. Core Data Contracts (`IconStyle`, `ContainerSpec`, `TreatmentSpec`)
- **`IconStyle`** (`SchemaVersion = 1`): Top-level immutable recipe combining `Id`, `Name`, `ContainerSpec`, and `TreatmentSpec`, serialized deterministically to camelCase JSON.
  - Default preset ("Soft Lavender"): `Squircle` container, `#E6DBFA` background, `padding = 0.16`, `saturation = -0.15`, `brightness = 0.05`, `contrast = 0.0`, `tintColor = null`.
- **`ContainerSpec`**:
  - `Shape`: `Squircle`, `RoundedSquare`, `Circle` (expandable on Android to include `Teardrop` / `Pebble` / `Hexagon` or Material 3 shapes).
  - `BackgroundColor`: Hex `#RRGGBB` or `#AARRGGBB`.
  - `Padding`: Clamped in `[0.0, 0.40]`, default `0.16` (meaning $1.0 - 2 \times 0.16 = 68\%$ inner safe bounding box).
- **`TreatmentSpec`**:
  - `Saturation`: Clamped in `[-1.0, 1.0]`.
  - `Brightness`: Clamped in `[-1.0, 1.0]`.
  - `Contrast`: Clamped in `[-1.0, 1.0]`.
  - `TintColor`: Optional `#RRGGBB` string.

### 1.2. Four-Stage Deterministic Rendering Pipeline (`IconStyleRenderer`)
In `windows-icons-admin`, `IconStyleRenderer.Render(source, style, canvasSize = 256)` executes four pure stages over an uncompressed RGBA byte buffer (`RgbaCanvas`):

1. **Effective Bounds Detection & Bilinear Normalization (`IconNormalizer`)**:
   - **Alpha Thresholding (`AlphaThreshold = 16`)**: Scans all pixels to find `(minX, minY, maxX, maxY)` where $\alpha \ge 16$, stripping invisible/near-zero padding halos around heterogeneous icons.
   - **Aspect-Preserving Scale & Centering**: Computes `cropWidth = maxX - minX + 1` and `cropHeight = maxY - minY + 1`. Given target canvas size $S$ and padding ratio $p \in [0.0, 0.40]$, computes inner box $I = S \times (1 - 2p)$ and uniform scale factor:
     $$\text{scale} = \min\left(\frac{I}{\text{cropWidth}}, \frac{I}{\text{cropHeight}}\right)$$
   - Centers the resampled rectangle at `dstX = (S - dstWidth) / 2`, `dstY = (S - dstHeight) / 2` using bilinear interpolation.
2. **Color & Tonal Treatment (`ApplyTreatment`)**:
   - Skips transparent pixels ($\alpha = 0$).
   - **Rec.709 Luminance Saturation**:
     $$Y = 0.2126 R + 0.7152 G + 0.0722 B$$
     $$C' = Y + (C - Y) \times (1.0 + \text{sat})$$
   - **Brightness Offset**: Adds $\text{bri} \times 255.0$ to each RGB channel.
   - **Contrast Curve**: With $\text{factor} = \frac{1.0 + \text{con}}{1.0 - 0.99 \times \text{con}}$, transforms each channel around midpoint $0.5$:
     $$C'' = \left(\left(\frac{C'}{255.0} - 0.5\right) \times \text{factor} + 0.5\right) \times 255.0$$
   - **Gentle Tint Blend (15%)**: If `TintColor` is present, blends $C''' = 0.85 C'' + 0.15 C_{\text{tint}}$, preserving icon recognizability and internal shading rather than flattening it into a solid silhouette (unless Monochrome mode is explicitly chosen on Android).
3. **Container Synthesis (`RenderContainer`)**:
   - Uses $4 \times 4$ subpixel supersampling (16 samples per pixel) in pure software or anti-aliased `Path`/`Canvas` on Android:
     - **Squircle**: Superellipse equation $\left|\frac{x - c_x}{r}\right|^{4.5} + \left|\frac{y - c_y}{r}\right|^{4.5} \le 1.0$.
     - **RoundedSquare**: Corner radius $r_c = 0.225 \times S$.
     - **Circle**: Euclidean distance $(x - c_x)^2 + (y - c_y)^2 \le r^2$.
4. **Porter-Duff `SRC_OVER` Alpha Compositing (`Composite`)**:
   - Composites the normalized, treated foreground over the rendered container background:
     $$\alpha_{\text{out}} = \alpha_f + \alpha_b (1 - \alpha_f), \quad C_{\text{out}} = \frac{C_f \alpha_f + C_b \alpha_b (1 - \alpha_f)}{\alpha_{\text{out}}}$$

### 1.3. Content-Addressed Caching & Transactional Undo (`RenderedIconCache` & `AppIconUndoStore`)
- **Cache Key**: `SHA-256(SHA-256(sourceBytes) + "|" + style.ToJson() + "|v" + RendererVersion)`. Ensures zero redundant re-rendering when previewing or re-applying styles.
- **Transactional Undo**: `AppIconUndoStore` persists up to 50 `AppBatchRecord` entries atomically (write to `.tmp`, then atomic rename/replace with `.bak` fallback on corruption), paired with `OriginalSnapshotStore` so every modified app icon can be restored individually, by batch, or globally.

---

## 2. Android Launcher Icons, Adaptive Icons & Themed Icons (API 26 to API 36 / Android 8.0 to 16)

### 2.1. `AdaptiveIconDrawable` Geometry (API 26 / Android 8.0+)
Introduced in Android 8.0 (API 26), `android.graphics.drawable.AdaptiveIconDrawable` separates an app icon into isolated layers rendered dynamically by the launcher:
- **Full Layer Size**: `108dp x 108dp` (`<background>` and `<foreground>` layers must both be sized at `108dp x 108dp`).
- **Inner Safe Zone (Visible Viewport)**: The central `72dp x 72dp` region (spanning from `18dp` to `90dp` on both axes). The outer `18dp` margin on all four sides is reserved for motion/parallax effects and clipping.
- **Optical Keyline Shapes**: Within the `72dp x 72dp` safe zone, standard keyline shapes occupy `66dp` (circle diameter) or `52dp x 52dp` (square) so visual weight remains balanced.
- **OEM Masking**: The system launcher calls `AdaptiveIconDrawable.getIconMask()` (or applies its own `Path` mask via `LauncherApps` / `IconNormalizer` in AOSP `Launcher3`) to crop the `108dp` composite into a circle, squircle, rounded square, or teardrop.

### 2.2. Monochrome Layer & System Themed Icons (API 33 to API 36 / Android 13 to 16)
- **API 33 (Android 13)**: Added the `<monochrome>` child element to `<adaptive-icon>` and the corresponding `AdaptiveIconDrawable.getMonochrome()` API.
  - When a user enables **Themed Icons** (Material You / Monet dynamic coloring) in system wallpaper settings, the launcher queries `adaptiveIcon.getMonochrome()`.
  - It tints the single-color silhouette drawable (`BlendMode.SRC_IN`) using the user's dynamic Material 3 tonal palette (typically `secondaryContainer` / `onSecondaryContainer` or `primary` tokens) over a pastel/dark surface container.
- **API 35 / API 36 (Android 15 & Android 16) Algorithmic Monochrome Fallback**:
  - In Android 13–14, if an app omitted `<monochrome>` (`getMonochrome() == null`), the launcher left the icon unthemed (breaking home screen visual cohesion).
  - Starting in Android 15/16 (`Launcher3` `MonochromeIconFactory`), the system synthesizes a monochrome icon on-the-fly for unadapted apps by rasterizing the foreground drawable, extracting luminance into an `ALPHA_8` mask, normalizing minimum/maximum luminance density, and applying a mid-tone contrast curve.
  - **Relevance to `AndroidIconsAdmin`**: Many users dislike the harsh high-contrast or accidentally inverted look of Android's forced monochrome icons, or use OEM launchers with inconsistent theming. `AndroidIconsAdmin` can both:
    1. Extract `getMonochrome()` when available for clean silhouette tinting, AND
    2. Apply `windows-icons-admin`'s gentle 15% tint + desaturation + pastel/dark container pipeline to full-color foregrounds so **100% of installed apps** achieve a cohesive, soft, recognizable aesthetic without color inversion artifacts.

### 2.3. Discovering Installed Launchable Apps & Package Visibility (API 30+)
Starting in Android 11 (API 30), package visibility filtering hides installed apps by default unless declared in `AndroidManifest.xml`.

#### `<queries>` vs `QUERY_ALL_PACKAGES` (Google Play Policy Critical Distinction)
- **`android.permission.QUERY_ALL_PACKAGES`**: Considered a high-risk/restricted permission by Google Play. Requires a formal Play Console declaration and is frequently rejected unless the app is a full system launcher, file manager, or security scanner.
- **`<queries>` with `ACTION_MAIN` + `CATEGORY_LAUNCHER` (Recommended & Play-Compliant)**:
  ```xml
  <queries>
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.LAUNCHER" />
      </intent>
      <!-- Optional: also discover installed third-party launchers & icon packs -->
      <intent>
          <action android:name="android.intent.action.MAIN" />
          <category android:name="android.intent.category.HOME" />
      </intent>
  </queries>
  ```
  - **Technical & Policy Reality**: Declaring `<intent>` with `ACTION_MAIN` and `CATEGORY_LAUNCHER` inside `<queries>` is a standard, unprivileged Android manifest declaration (not a runtime or restricted permission). It makes **100% of user-launchable apps** visible to `PackageManager.queryIntentActivities()` and `LauncherApps.getActivityList()` without requiring `QUERY_ALL_PACKAGES`.

#### Extracting Raw Unmasked Icon Layers (`LauncherApps` & `PackageManager`)
To prevent "double-container" artifacts (putting an already-masked OEM squircle inside another squircle container), `AndroidIconsAdmin` should extract raw drawables with the following priority:
1. Query launchable activities via `LauncherApps.getActivityList(null, Process.myUserHandle())` or `PackageManager.queryIntentActivities(mainLauncherIntent, PackageManager.MATCH_ALL)`.
2. Load the raw unmasked resource directly from the target package's `Resources` (`packageManager.getResourcesForApplication(appInfo)` -> `resources.getDrawableForDensity(iconResId, DisplayMetrics.DENSITY_XXXHIGH, null)`) or `packageManager.getApplicationIcon(packageName)`.
   - *Note*: Avoid `LauncherActivityInfo.getBadgedIcon()` as the primary source if we want the raw unmasked `AdaptiveIconDrawable`, because `getBadgedIcon()` on some OEM ROMs returns a pre-flattened `BitmapDrawable` with the OEM's shape mask already baked in.
3. Inspect the loaded `Drawable`:
   - **Case 1 (`drawable is AdaptiveIconDrawable`)**:
     - We have direct access to `drawable.foreground`, `drawable.background`, and (on API 33+) `drawable.monochrome`.
     - **Foreground-Only Extraction Mode**: Render `drawable.foreground` onto a transparent canvas (accounting for the `18dp` adaptive inset so the inner `72dp` safe zone maps to the full canvas before `IconNormalizer` crops transparent bounds). If the foreground relies on the original background for internal cutouts (e.g., transparent logo cutout over a colored background), provide a toggle per-app or automatic edge-alpha heuristic to either use `foreground` alone or composite `background + foreground` before cropping/masking.
   - **Case 2 (`drawable is BitmapDrawable` or `VectorDrawable` - Legacy Icon)**:
     - Rasterize directly to an `ARGB_8888` `Bitmap`, then run `IconNormalizer.Normalize()` to strip transparent margins and center it inside the target `ContainerSpec`.

---

## 3. Hard Android OS Security Boundaries & Viable Delivery Mechanisms

### 3.1. What a Normal Non-Root Android App CANNOT Do
Due to Android's Linux UID application sandboxing, SELinux mandatory access control, and APK v2/v3/v4 cryptographic signature verification:
1. **Cannot Mutate Another App's APK**: Every installed APK resides in `/data/app/<package>/base.apk` owned by `system` and verified by `fs-verity` / APK Signature Scheme. No non-root app can rewrite another app's `res/mipmap` resources.
2. **Cannot Overwrite Stock Launcher Databases or Caches**: Stock system launchers (Pixel Launcher `com.google.android.apps.nexuslauncher`, Samsung One UI Home `com.sec.android.app.launcher`, Xiaomi MIUI/HyperOS Home) store their app drawer icon caches (`app_icons.db`) inside their own private `/data/data/<launcher_package>/` sandbox. A third-party app without root or system-level Runtime Resource Overlays (RRO / Shizuku) cannot overwrite icons inside the stock launcher's App Drawer.

### 3.2. What a Normal Non-Root Android App CAN Do (Three Concrete Mechanisms)

Because `AndroidIconsAdmin` targets standard unrooted Android devices, it must be architected around the **three legitimate, OS-sanctioned mechanisms** for delivering styled icons, with crystal-clear UX explaining how each works:

---

#### Mechanism A: Pinned Home Screen Shortcuts (`ShortcutManagerCompat` / Widget Proxy)
Works on **100% of Android launchers**, including locked-down stock OEM launchers (Pixel Launcher, Samsung One UI Home, Xiaomi, Motorola, OPPO/ColorOS, Nothing OS).

- **How It Works**:
  - Uses `ShortcutInfoCompat.Builder(context, shortcutId)` and `ShortcutManagerCompat.requestPinShortcut()` (wrapping Android 8.0+ `android.content.pm.ShortcutManager`).
  - The shortcut's `IconCompat` is set to the styled `Bitmap` (`IconCompat.createWithBitmap(styledBitmap)` or `createWithAdaptiveBitmap()`), and its `Intent` resolves via a lightweight trampoline activity (`ShortcutLaunchActivity` with `android:theme="@android:style/Theme.NoDisplay"` and `excludeFromRecents="true"`) or directly fires the target app's main `ACTION_MAIN` `ComponentName` intent.
  - **Updating Existing Pinned Shortcuts Instantly (`updateShortcuts`)**:
    - **Crucial Advantage**: Once a shortcut is pinned to the Home Screen and owned by `AndroidIconsAdmin`, calling `ShortcutManagerCompat.updateShortcuts(context, listOfUpdatedShortcutInfos)` updates its icon **in-place, silently, and in batches with ZERO confirmation dialogs** whenever the user switches to a new style preset (e.g., from "Soft Lavender" to "Midnight Slate") or reverts to the original icon!
- **OS Constraints & Realities**:
  1. **Initial Pinning Confirmation**: On Android 8.0+, calling `requestPinShortcut()` shows a system confirmation sheet per shortcut (or the user can drag-and-drop from our app / add via a custom Home Screen `AppWidgetProvider` 1x1 icon widget which skips the shortcut badge altogether).
  2. **Home Screen Scope**: Pinned shortcuts live on the Home Screen (they do not replace the entry inside the App Drawer).
  3. **The Creator App Badge & How to Handle It**:
     - Shortcuts pinned via `ShortcutManager.requestPinShortcut()` receive a small overlay badge in the bottom-right corner showing the creator app's icon (`AndroidIconsAdmin`).
     - **Mitigation 1 (1x1 Custom Icon AppWidget / Grid Widget)**: Home screen widgets (`AppWidgetProvider`) rendered by `AndroidIconsAdmin` have **NO system badge**, can display single 1x1 icons or 4x1 / 4x2 themed icon folders/docks on any stock launcher (including Pixel Launcher and One UI), and update 100% silently in bulk whenever a style preset changes!
     - **Mitigation 2 (Legacy `com.android.launcher.action.INSTALL_SHORTCUT` / `ACTION_CREATE_SHORTCUT` picker)**: Supported when initiated from the launcher's "Add Widget -> Shortcut" flow on several launchers without a badge.

---

#### Mechanism B: Third-Party Launcher Icon Pack Protocol & Exported Companion APK
Supported by almost all customizable third-party launchers (Nova Launcher, Lawnchair, Niagara Launcher, Smart Launcher, Action Launcher, Hyperion, Apex, Fossify Launcher, Neo Launcher) and Samsung One UI (via Good Lock *Theme Park*).

- **How Launchers Read Icon Packs**:
  - Compatible launchers discover installed icon packs by querying `PackageManager` for intent actions/categories such as:
    - `org.adw.launcher.THEMES`
    - `com.novalauncher.THEME`
    - `com.lawnchair.ICONPACK`
    - `ginlemon.smartlauncher.THEMES`
  - The launcher opens the icon pack APK's `Resources` and `AssetManager` (`getResourcesForApplication(iconPackPackageName)`) and parses:
    1. **`res/xml/appfilter.xml` (or `assets/appfilter.xml`)**: Maps each installed app's `ComponentName` (`ComponentInfo{package/activity}`) to a compiled drawable name (`drawable="icon_..."`), plus optional universal fallback rules (`<iconback>`, `<iconmask>`, `<iconupon>`, `<scale factor="0.85" />`).
    2. **`res/xml/drawable.xml`**: Lists all drawable names categorized for the launcher's manual icon picker UI.
    3. **`res/xml/theme_resources.xml`**: Supplemental mapping format used by Samsung / LG / custom theme parsers.
- **Crucial Technical Constraint (Static APK Resources vs. Dynamic Styling)**:
  - Because third-party launchers read drawables via `Resources.getIdentifier()` / `assets/appfilter.xml` inside an **installed APK**, an already-installed app **cannot** mutate its own compiled `res/drawable-nodpi/` or `assets/appfilter.xml` at runtime.
  - **How Dynamic Icon Pack Apps (like Icon Pack Studio) Solve This**:
    1. **Universal Masking (`iconback` / `iconmask` / `iconupon` + `scale`)**: The main `AndroidIconsAdmin` APK can include pre-compiled container shapes and masks in its own `res/xml/appfilter.xml` so launchers immediately see `AndroidIconsAdmin` as an icon pack for basic container masking.
    2. **On-Device Companion Icon Pack APK Generator or Pack Archive Export**: To deliver per-app custom-rendered bitmaps (`IconStyleRenderer` output for every installed app on the user's phone) to a third-party launcher, the app either generates a lightweight, aligned, v1/v2-signed `.apk` file containing `assets/appfilter.xml` and `res/drawable-nodpi/*.png` and installs it via Android's standard `PackageInstaller` session API (`REQUEST_INSTALL_PACKAGES` / user confirmation), OR exports a standard Icon Pack ZIP / PNG bundle.

---

#### Mechanism C: Asset Export & Manual / Semi-Automatic Application
- **How It Works**:
  - Exports high-resolution (`256x256` or `512x512` PNG / lossless WebP) styled icons—either individually via Android Share Sheet / `MediaStore` / Storage Access Framework (`ActivityResultContracts.CreateDocument`), or as a complete structured ZIP archive containing:
    - `/icons/<package_name>.png`
    - `/appfilter.xml`
    - `/drawable.xml`
    - `/manifest.json` (the `IconStyle` recipe + app mapping metadata)
  - Users on launchers that support per-app gallery icon selection (Nova, Lawnchair, Niagara, Action, One UI Theme Park) or custom shortcut makers can apply the exported PNGs directly, or import the ZIP into desktop/CLI APK packagers.

---

## 4. Reversibility & Restoration Architecture (`UndoStore` & State Recovery)

Just as `windows-icons-admin` guarantees non-destructive operation and one-click restoration via `AppIconUndoStore` and `OriginalSnapshotStore`, `AndroidIconsAdmin` defines concrete restoration semantics across every delivery layer:

| Layer / Mechanism | How Application Works | How Restoration / Undo Works |
| :--- | :--- | :--- |
| **1. Internal App Database & Preview State** | Stores raw extracted original icon layers in `OriginalSnapshotStore` (`filesDir/snapshots/<package>.png`) and records every batch or single-app style application in `AppIconUndoStore` (Room / atomic JSON). | **100% Instant & Deterministic**: Reverting a single app, a specific `AppBatchRecord`, or `RevertAll()` restores the app's assigned style state to `Original / Unstyled` in the local store. |
| **2. Home Screen Widgets (1x1 Icon & Folder/Row Widgets)** | `AppWidgetManager.updateAppWidget()` renders the styled icon bitmap from `RenderedIconCache` onto the `RemoteViews` `ImageView` on the user's Home Screen. | **100% Instant, Silent & Automatic**: Calling `RevertApp()`, `RevertBatch()`, or `RevertAll()` inside `AndroidIconsAdmin` immediately pushes the unstyled original icon snapshot from `OriginalSnapshotStore` to all active `AppWidgetProvider` instances via `AppWidgetManager` with **zero user prompts**. |
| **3. Pinned Home Screen Shortcuts (`ShortcutManagerCompat`)** | Pinned via `requestPinShortcut()` pointing to our trampoline/target intent with a styled `IconCompat` bitmap. | **100% Instant & Silent In-Place Visual Restore**: Because `AndroidIconsAdmin` owns the pinned shortcut IDs (`shortcut_<package>`), calling `RevertApp()`, `RevertBatch()`, or `RevertAll()` invokes `ShortcutManagerCompat.updateShortcuts()` to replace the shortcut's bitmap with the **original unstyled app icon** from `OriginalSnapshotStore` silently in the background! Alternatively, `ShortcutManagerCompat.disableShortcuts()` can disable managed shortcuts with a custom message. |
| **4. Exported Icon Pack / Third-Party Launcher** | Applied in third-party launcher settings or exported to storage. | Restored by switching the third-party launcher's icon pack setting back to "System Default" or regenerating/updating the pack after reverting batch rules. |

> **Architectural Insight on Restoration**: By using **ID-stable Pinned Shortcuts** (`ShortcutManagerCompat.updateShortcuts`) and **1x1 / Grid Home Screen Widgets** (`AppWidgetManager`), `AndroidIconsAdmin` achieves true **in-place batch apply and batch undo on the Home Screen without re-prompting the user**, closely mirroring the transactional UX of `windows-icons-admin`!

---

## 5. Recommended Modern Android Tech Stack (2026 Stable Baseline)

To mirror `windows-icons-admin`'s Cleanroom TDD architecture (`Core` pure domain + `App` UI shell) and zero-cloud/zero-telemetry principles:

### 5.1. Language, Build System & SDK Targets
- **Language**: **Kotlin `2.1.20`** (or `2.2.0` stable K2 compiler) with Coroutines & Flow (`kotlinx.coroutines` `1.10.1`) and `kotlinx.serialization-json` (`1.8.0`).
- **Build System**: **Android Gradle Plugin (AGP) `8.8.x` / `8.9.x`** with Gradle `8.11+` and Version Catalog (`gradle/libs.versions.toml`).
- **SDK Levels**:
  - `minSdk = 26` (Android 8.0 Oreo — baseline for both `AdaptiveIconDrawable` and `ShortcutManager.requestPinShortcut`).
  - `targetSdk = 35` / `compileSdk = 35` (Android 15, ready for API 36 Android 16 edge-to-edge and monochrome specifications).

### 5.2. UI, Persistence & Image Processing Stack
- **UI Toolkit**: **Jetpack Compose (BOM `2025.02.00`+ / Material 3 `1.3.1`+)**
  - Native edge-to-edge layout (`enableEdgeToEdge()`), adaptive navigation, live interactive sliders for `Padding`, `Saturation`, `Brightness`, `Contrast`, `Shape`, and `TintColor`, plus real-time multi-icon preview grid.
- **Home Screen Widgets**: **Jetpack Glance (`androidx.glance:glance-appwidget:1.1.1`)** or standard `AppWidgetProvider` (`RemoteViews`) for zero-badge 1x1 custom icon widgets and 4x1 themed dock widgets that update instantaneously on batch apply/undo.
- **Persistence**:
  - **Room (`androidx.room:room-runtime:2.7.0` / `2.8.x` with KSP)** OR atomic JSON stores (mirroring `StyleStore`, `RuleStore`, and `AppIconUndoStore` in `windows-icons-admin` using `kotlinx.serialization`) paired with **Jetpack DataStore Preferences (`1.1.3` / `1.2.x`)** for `AppSettings`.
- **Deterministic Image Processing (100% Offline, Zero Cloud/AI Dependencies)**:
  - **Pure Kotlin `RgbaCanvas` + `IconNormalizer` + `IconStyleRenderer` in `:core` module**:
    - By keeping `RgbaCanvas` (byte/int array pixel buffer), `IconNormalizer` (alpha-threshold bounding box + bilinear resampling), `IconStyleRenderer` (Rec.709 saturation, brightness, contrast, 15% tint, $4\times 4$ supersampled `Squircle` / `RoundedSquare` / `Circle` / `Teardrop` containers, Porter-Duff `SRC_OVER`), `RuleEngine`, and `AppIconUndoStore` as **pure Kotlin classes with zero `android.*` dependencies**, the entire `:core` module can run blazingly fast on standard JVM JUnit 5 tests (`./gradlew :core:test`) in milliseconds—just like `WindowsIconsAdmin.Core.Tests`!
  - **Android Bitmap Bridge (in `:app` / platform layer)**:
    - Converts Android `Drawable` (`AdaptiveIconDrawable` foreground/monochrome or `BitmapDrawable`) to `RgbaCanvas` via `Bitmap.getPixels()` / `Bitmap.copyPixelsToBuffer()`, and converts rendered `RgbaCanvas` back to `android.graphics.Bitmap` (`Bitmap.Config.ARGB_8888`) and PNG streams (`Bitmap.compress(CompressFormat.PNG, 100, out)`).

---

## 6. Samsung One UI 8.5 (Android 16 / API 36) & Good Lock Theme Park Deep-Dive

### 6.1. One UI 8.5 Baseline & Android 16 (API 36) Requirements
**Samsung One UI 8.5** runs atop **Android 16 (API level 36)**. To guarantee first-class compatibility on Galaxy devices running One UI 8.0 and One UI 8.5:
- **SDK Configuration**: `compileSdk = 36` and `targetSdk = 36` (with backward compatibility down to `minSdk = 26`).
- **Mandatory Edge-to-Edge Enforcement (API 36)**: Android 16 removes legacy opt-out flags (`windowOptOutEdgeToEdgeEnforcement`). The Jetpack Compose UI must natively use `enableEdgeToEdge()` with proper `WindowInsets` (`systemBars`, `navigationBars`, `statusBars`) padding.
- **Predictive Back Animations**: Enabled by default on API 36 (`android:enableOnBackInvokedCallback="true"`).
- **One UI Squircle Geometry**: Samsung's native icon grid uses a distinctive superellipse ("One UI Squircle"). Including a dedicated `ONE_UI_SQUIRCLE` / `SQUIRCLE` container preset and matching optical padding ensures custom widgets and shortcuts blend seamlessly into One UI 8.5 Home Screens and enlarged folders.

### 6.2. How One UI 8.5 Home & Good Lock *Theme Park* Work with Custom Icons
Samsung's stock launcher (`com.sec.android.app.launcher` — One UI Home) does not natively read raw `appfilter.xml` files from loose directories, nor does it allow third-party apps to overwrite its internal SQLite icon database directly. Instead, One UI 8.5 supports **four complementary customization workflows**:

1. **Badge-Free Home Screen Widgets (`AppWidgetProvider` with One UI 8.5 *Home Up* Scaling)**:
   - One UI 8.5 integrates enhanced widget scaling and independent resizing (via *Home Up* and native One UI 8.5 long-press widget controls), allowing 1×1 and 4×1 icon widgets to match the exact visual scale of native One UI Home icons.
   - `AndroidIconsAdmin` widgets (`targetCellWidth="1"`, `targetCellHeight="1"`, `resizeMode="horizontal|vertical"`) display zero creator badges and update **100% silently and instantaneously in bulk** whenever a style or undo action is triggered.
2. **Pinned Shortcuts (`ShortcutManagerCompat`)**:
   - Fully supported on One UI 8.5 Home Screen, with instant silent bulk updates and restoration via `ShortcutManagerCompat.updateShortcuts()`.
3. **Good Lock *Theme Park* — Direct Icon Pack APK & Universal Masking**:
   - Samsung's official **Theme Park** module (Good Lock) allows applying third-party Icon Packs to the **entire One UI 8.5 system** (including the App Drawer, Home Screen, Edge Panels, and Recent Apps).
   - **Discovery Contract**: Theme Park discovers installed icon packs by scanning `PackageManager` for standard icon pack intent filters in `AndroidManifest.xml` (`org.adw.launcher.THEMES`, `com.novalauncher.THEME`, `com.gau.go.launcherex.theme`) and `<meta-data android:name="org.adw.launcher.theme.ICS_ICON_PACK" android:resource="@xml/appfilter" />`.
   - **Resource Indexing Constraint**: Theme Park inspects compiled APK resources (`resources.arsc` + `res/xml/appfilter.xml` and `assets/appfilter.xml`). By declaring these intent filters and bundling universal `<iconback>`, `<iconmask>`, and `<scale>` drawables in `AndroidIconsAdmin`'s manifest and `res/xml/appfilter.xml`, `AndroidIconsAdmin` appears directly in Theme Park's *"Icon Pack"* selector.
4. **Good Lock *Theme Park* & Gallery Per-App PNG Export (`MediaStore` / `Pictures/AndroidIconsAdmin/`)**:
   - In One UI 8.5, Theme Park includes a per-app icon editor (*Icon -> Create New -> Tap 3-dots / App Icon -> Change Icon from Gallery*) and supports new One UI 8.5 surface effects (*Basic, Glass, Duotone, Gradient*).
   - `AndroidIconsAdmin` provides a dedicated **"Export to Gallery for One UI Theme Park"** action that writes high-resolution (`512×512` transparent PNG) styled icons directly into a dedicated album (`Pictures/AndroidIconsAdmin/<StyleName>/` via `MediaStore.Images.Media` with `RELATIVE_PATH`, requiring **zero storage permissions** on Android 10–16 / API 29–36), alongside the standard `appfilter.xml` + `drawable.xml` ZIP bundle.

---

## 7. External References & Official Documentation Links

1. **Android Developers — Adaptive Icons**: [https://developer.android.com/develop/ui/views/launch/icon_design_adaptive](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive)
2. **Android Developers — `AdaptiveIconDrawable` API Reference (API 26–36)**: [https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable](https://developer.android.com/reference/android/graphics/drawable/AdaptiveIconDrawable)
3. **Android Developers — Package Visibility & `<queries>` Declaring (`ACTION_MAIN` / `CATEGORY_LAUNCHER`)**: [https://developer.android.com/training/package-visibility/declaring](https://developer.android.com/training/package-visibility/declaring)
4. **Android Developers — Pinned Shortcuts & `ShortcutManagerCompat`**: [https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#pinned](https://developer.android.com/develop/ui/views/launch/shortcuts/creating-shortcuts#pinned)
5. **Google Play Console Help — Use of Broad Package (`QUERY_ALL_PACKAGES`) Permission Policy**: [https://support.google.com/googleplay/android-developer/answer/10158779](https://support.google.com/googleplay/android-developer/answer/10158779)
6. **AOSP `Launcher3` `IconNormalizer` & `MonochromeIconFactory` Source**: [https://cs.android.com/android/platform/superproject/main/+/main:frameworks/libs/systemui/iconloaderlib/src/com/android/launcher3/icons/](https://cs.android.com/android/platform/superproject/main/+/main:frameworks/libs/systemui/iconloaderlib/src/com/android/launcher3/icons/)
7. **Android 16 (API 36) Behavior Changes & Mandatory Edge-to-Edge**: [https://developer.android.com/about/versions/16/behavior-changes-16](https://developer.android.com/about/versions/16/behavior-changes-16)
8. **Samsung Good Lock & Theme Park Icon Customization on One UI 8 / 8.5**: [https://www.samsung.com/us/support/answer/ANS00083043/](https://www.samsung.com/us/support/answer/ANS00083043/)
