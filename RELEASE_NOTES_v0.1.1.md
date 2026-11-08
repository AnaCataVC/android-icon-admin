## AndroidIconsAdmin v0.1.1

Hotfix release addressing the startup crash on Android devices (Samsung One UI / AOSP) and improving layer extraction and state resilience.

### What's New & Fixes
- **Pure Kotlin PNG Codec (:core)**: Completely replaced desktop Java AWT (`BufferedImage`) and `ImageIO` with a pure Kotlin PNG encoder and decoder using standard `java.util.zip` (`Deflater`, `Inflater`, and `CRC32`). Resolves the critical `NoClassDefFoundError: java.awt.image.BufferedImage` that caused the app to crash immediately upon launch on Android.
- **Hardware Bitmap Protection (:app)**: Streamlined `AdaptiveLayerExtractor` to render drawables onto software `ARGB_8888` canvases, avoiding `Config#HARDWARE` pixel access exceptions when inspecting app icons.
- **Fail-Safe Loading Boundaries (:app)**: Isolated icon extraction and thumbnail rendering with error boundaries in `MainViewModel` and `SingleIconWidgetProvider`; corrupted or unsupported icons in third-party apps are skipped cleanly without crashing the app.
- **Resilient Bitmap Canvas Bridge**: Enhanced `BitmapCanvasBridge` with fallback allocation if `bitmap.copy(...)` returns null.

---

### Novedades y Correcciones (Español)
- **Códec PNG en Kotlin Puro (:core)**: Reemplazo integral de APIs de escritorio Java AWT (`BufferedImage`) e `ImageIO` por un codificador y decodificador PNG nativo en Kotlin puro utilizando `java.util.zip` (`Deflater`, `Inflater` y `CRC32`). Soluciona el fallo crítico `NoClassDefFoundError: java.awt.image.BufferedImage` que provocaba el cierre de la app al iniciar en Android.
- **Protección contra Bitmaps de Hardware (:app)**: Optimización en `AdaptiveLayerExtractor` para renderizar iconos directamente en un lienzo software `ARGB_8888`, eliminando excepciones por acceso a mapas de bits en hardware (`Config#HARDWARE`).
- **Aislamiento de Errores en Carga y Widgets (:app)**: Procesamiento seguro y tolerante a fallos en `MainViewModel` y `SingleIconWidgetProvider`; si una app del dispositivo presenta recursos corruptos o atípicos, se descarta limpiamente sin bloquear el lanzamiento de la app.
- **Puente de Canvas Seguro**: Mayor robustez en `BitmapCanvasBridge` con reserva de respaldo ante conversiones nulas o incompatibles.
