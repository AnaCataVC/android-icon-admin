[English](#english) | [Español](#español)

<a name="english"></a>
# Release v0.1.2

## Features
- **Real-Time Live Preview**: The Style Studio now dynamically renders icon previews as shapes, background colors, padding margins, treatments, and extraction modes change.
- **Sample Preview Icons**: Added built-in vector preview fallbacks (Phone, Messages, Camera, Music, Settings) ensuring the live preview is immediately interactive even before apps are scanned.

## Bug Fixes
- **Strict Silhouette Masking**: Fixed an issue where icon foregrounds with opaque edges bled outside non-square shapes (Squircle, One UI 8.5, Circle, Teardrop). `IconStyleRenderer` now strictly masks the composite output against the container shape silhouette.
- **Preset Preservation**: Fixed a defect in `StyleStore.getAll()` where saving a custom or modified style removed standard presets from the available list. Standard presets are now always retained and merged in-place.
- **Multi-Mode Snapshot Storage**: Added multi-mode caching in `OriginalSnapshotStore` and `MainViewModel` to properly extract and preserve icon layers when switching extraction modes (such as Android 13+ Monochrome).

## Maintenance
- **Regression Test Suite**: Added automated JUnit 5 tests covering preset preservation in `StyleStore`, strict container shape masking in `IconStyleRenderer.composite`, and extraction mode snapshot caching.
- **Architecture Documentation**: Documented technical decisions in ADR 0002 regarding reactive previews, silhouette masking, and snapshot persistence.

---

<a name="español"></a>
# Lanzamiento v0.1.2

## Nuevas Funcionalidades
- **Vista Previa en Vivo en Tiempo Real**: El Estudio de Estilos ahora renderiza de forma dinámica la vista previa de iconos al cambiar formas, colores de fondo, margen/padding, tratamientos y modos de extracción.
- **Iconos de Muestra para Previsualización**: Se incluyeron iconos vectoriales de respaldo (Teléfono, Mensajes, Cámara, Música, Ajustes) garantizando interactividad inmediata en la vista previa incluso antes de cargar aplicaciones.

## Correcciones de Errores
- **Máscara Estricta de Silueta**: Corregido el problema donde los bordes opacos del icono sangraban fuera de formas no cuadradas (Squircle, One UI 8.5, Círculo, Teardrop). `IconStyleRenderer` ahora recorta estrictamente la imagen compuesta a la silueta del contenedor.
- **Preservación de Presets**: Corregido un fallo en `StyleStore.getAll()` donde guardar un estilo borraba los presets estándar de la lista disponible. Los presets estándar ahora se mantienen siempre accesibles y se combinan con estilos personalizados.
- **Almacenamiento Multi-Modo de Snapshots**: Implementado soporte multi-modo en `OriginalSnapshotStore` y `MainViewModel` para extraer y conservar las capas correctas al cambiar de modo de extracción (como Monocromo en Android 13+).

## Mantenimiento
- **Suite de Pruebas de Regresión**: Incorporadas pruebas unitarias JUnit 5 que validan la preservación de presets, el enmascaramiento estricto de siluetas y la persistencia de snapshots por modo.
- **Documentación de Arquitectura**: Registradas las decisiones técnicas en el ADR 0002 respecto al renderizado reactivo, recorte de siluetas y persistencia.
