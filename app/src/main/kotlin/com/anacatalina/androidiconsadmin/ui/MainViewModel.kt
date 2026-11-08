package com.anacatalina.androidiconsadmin.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anacatalina.androidiconsadmin.core.history.AppBatchRecord
import com.anacatalina.androidiconsadmin.core.history.AppIconChangeEntry
import com.anacatalina.androidiconsadmin.core.history.AppIconUndoStore
import com.anacatalina.androidiconsadmin.core.history.OriginalSnapshotStore
import com.anacatalina.androidiconsadmin.core.rules.AppRule
import com.anacatalina.androidiconsadmin.core.rules.RuleEngine
import com.anacatalina.androidiconsadmin.core.rules.RuleStore
import com.anacatalina.androidiconsadmin.core.settings.AppSettings
import com.anacatalina.androidiconsadmin.core.settings.AppSettingsService
import com.anacatalina.androidiconsadmin.core.styling.IconStyle
import com.anacatalina.androidiconsadmin.core.styling.IconStyleRenderer
import com.anacatalina.androidiconsadmin.core.styling.RenderedIconCache
import com.anacatalina.androidiconsadmin.core.styling.StyleStore
import com.anacatalina.androidiconsadmin.platform.AdaptiveLayerExtractor
import com.anacatalina.androidiconsadmin.platform.AppDiscoveryService
import com.anacatalina.androidiconsadmin.platform.BitmapCanvasBridge
import com.anacatalina.androidiconsadmin.platform.IconPackExporter
import com.anacatalina.androidiconsadmin.platform.InstalledAppInfo
import com.anacatalina.androidiconsadmin.platform.ShortcutDeliveryService
import com.anacatalina.androidiconsadmin.widget.SingleIconWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AppItemUiState(
    val info: InstalledAppInfo,
    val originalBitmap: Bitmap?,
    val styledBitmap: Bitmap?,
    val assignedStyleId: String?,
    val isSelected: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val filesDir = context.filesDir

    private val settingsService = AppSettingsService(File(filesDir, "settings.json"))
    val styleStore = StyleStore(File(filesDir, "styles"))
    val ruleStore = RuleStore(File(filesDir, "rules.json"))
    val snapshotStore = OriginalSnapshotStore(File(filesDir, "snapshots"))
    val undoStore = AppIconUndoStore(File(filesDir, "undo.json"))
    val iconCache = RenderedIconCache(File(context.cacheDir, "rendered_icons"))

    private val _settings = MutableStateFlow(settingsService.loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _styles = MutableStateFlow(styleStore.getAll())
    val styles: StateFlow<List<IconStyle>> = _styles.asStateFlow()

    private val _activeStyle = MutableStateFlow(styleStore.get(_settings.value.activeStyleId) ?: IconStyle.createDefault())
    val activeStyle: StateFlow<IconStyle> = _activeStyle.asStateFlow()

    private val _rules = MutableStateFlow(ruleStore.loadRules())
    val rules: StateFlow<List<AppRule>> = _rules.asStateFlow()

    private val _apps = MutableStateFlow<List<AppItemUiState>>(emptyList())
    val apps: StateFlow<List<AppItemUiState>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _history = MutableStateFlow(undoStore.getHistory())
    val history: StateFlow<List<AppBatchRecord>> = _history.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            withContext(Dispatchers.IO) {
                val installed = try {
                    AppDiscoveryService.getInstalledLaunchableApps(
                        context,
                        includeSystemApps = _settings.value.showSystemApps
                    )
                } catch (_: Throwable) {
                    emptyList()
                }

                val items = installed.mapNotNull { app ->
                    try {
                        // 1. Get or extract original snapshot
                        var originalCanvas = snapshotStore.getSnapshot(app.packageName, _activeStyle.value.extractionMode.name)
                        if (originalCanvas == null) {
                            originalCanvas = AdaptiveLayerExtractor.extractIcon(
                                context,
                                app.packageName,
                                _activeStyle.value.extractionMode,
                                targetSize = 256
                            )
                            if (originalCanvas != null) {
                                snapshotStore.saveSnapshot(app.packageName, originalCanvas, _activeStyle.value.extractionMode.name)
                                snapshotStore.saveSnapshot(app.packageName, originalCanvas)
                            }
                        }

                        val originalBmp = (originalCanvas ?: snapshotStore.getSnapshot(app.packageName))?.let {
                            BitmapCanvasBridge.toBitmap(it)
                        }

                        // 2. Render styled preview
                        val styledBmp = originalCanvas?.let {
                            val styledCanvas = IconStyleRenderer.render(it, _activeStyle.value, 256)
                            BitmapCanvasBridge.toBitmap(styledCanvas)
                        }

                        AppItemUiState(
                            info = app,
                            originalBitmap = originalBmp,
                            styledBitmap = styledBmp,
                            assignedStyleId = _activeStyle.value.id
                        )
                    } catch (_: Throwable) {
                        null
                    }
                }
                _apps.value = items
            }
            _isLoading.value = false
        }
    }

    fun selectActiveStyle(style: IconStyle) {
        _activeStyle.value = style
        _settings.value = _settings.value.copy(activeStyleId = style.id)
        settingsService.saveSettings(_settings.value)
        refreshPreviews(style)
    }

    fun saveActiveStyle(updatedStyle: IconStyle) {
        styleStore.save(updatedStyle)
        _styles.value = styleStore.getAll()
        _activeStyle.value = updatedStyle
        _settings.value = _settings.value.copy(activeStyleId = updatedStyle.id)
        settingsService.saveSettings(_settings.value)
        refreshPreviews(updatedStyle)
    }

    fun refreshPreviews(targetStyle: IconStyle? = null) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val style = targetStyle ?: _activeStyle.value
                val current = _apps.value
                val updated = current.map { item ->
                    try {
                        var originalCanvas = snapshotStore.getSnapshot(item.info.packageName, style.extractionMode.name)
                        if (originalCanvas == null) {
                            originalCanvas = AdaptiveLayerExtractor.extractIcon(
                                context,
                                item.info.packageName,
                                style.extractionMode,
                                targetSize = 256
                            )
                            if (originalCanvas != null) {
                                snapshotStore.saveSnapshot(item.info.packageName, originalCanvas, style.extractionMode.name)
                            } else {
                                originalCanvas = snapshotStore.getSnapshot(item.info.packageName)
                            }
                        }

                        val styledBmp = originalCanvas?.let {
                            val styledCanvas = IconStyleRenderer.render(it, style, 256)
                            BitmapCanvasBridge.toBitmap(styledCanvas)
                        }
                        item.copy(
                            styledBitmap = styledBmp,
                            assignedStyleId = style.id
                        )
                    } catch (_: Throwable) {
                        item
                    }
                }
                _apps.value = updated
            }
        }
    }

    fun toggleAppSelection(packageName: String) {
        _apps.value = _apps.value.map {
            if (it.info.packageName == packageName) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllApps(select: Boolean) {
        _apps.value = _apps.value.map { it.copy(isSelected = select) }
    }

    fun applyStyleToSelected(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            var appliedCount = 0
            withContext(Dispatchers.IO) {
                val selectedApps = _apps.value.filter { it.isSelected }
                val targetApps = if (selectedApps.isNotEmpty()) selectedApps else _apps.value

                val changes = mutableListOf<AppIconChangeEntry>()
                val shortcutUpdates = mutableListOf<Triple<String, String, Bitmap>>()

                for (app in targetApps) {
                    val styledBmp = app.styledBitmap ?: continue
                    changes.add(
                        AppIconChangeEntry(
                            packageName = app.info.packageName,
                            previousStyleId = app.assignedStyleId,
                            newStyleId = _activeStyle.value.id,
                            isCustomized = true
                        )
                    )
                    shortcutUpdates.add(Triple(app.info.packageName, app.info.appLabel, styledBmp))
                    appliedCount++
                }

                if (changes.isNotEmpty()) {
                    val batch = AppBatchRecord(
                        batchId = "batch_${System.currentTimeMillis()}",
                        timestampMillis = System.currentTimeMillis(),
                        description = "Aplicado estilo '${_activeStyle.value.name}' a $appliedCount apps",
                        changes = changes
                    )
                    undoStore.recordBatch(batch)
                    _history.value = undoStore.getHistory()

                    if (_settings.value.autoSyncShortcutsOnApply) {
                        ShortcutDeliveryService.updateBatchShortcutsSilently(context, shortcutUpdates)
                    }

                    if (_settings.value.autoSyncWidgetsOnApply) {
                        SingleIconWidgetProvider.updateAllWidgets(context)
                    }
                }
            }
            _isLoading.value = false
            onComplete(appliedCount)
        }
    }

    fun pinAppShortcut(app: AppItemUiState, onResult: (Boolean) -> Unit) {
        val bmp = app.styledBitmap ?: app.originalBitmap ?: return
        val result = ShortcutDeliveryService.requestPinShortcut(
            context,
            app.info.packageName,
            app.info.appLabel,
            bmp
        )
        onResult(result)
    }

    fun revertLatestBatch(onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            var revertedDesc: String? = null
            withContext(Dispatchers.IO) {
                val batch = undoStore.popLatestBatch()
                if (batch != null) {
                    revertedDesc = batch.description
                    val restoreUpdates = mutableListOf<Triple<String, String, Bitmap>>()

                    for (change in batch.changes) {
                        val originalCanvas = snapshotStore.getSnapshot(change.packageName)
                        if (originalCanvas != null) {
                            val originalBmp = BitmapCanvasBridge.toBitmap(originalCanvas)
                            restoreUpdates.add(Triple(change.packageName, change.packageName, originalBmp))
                        }
                    }

                    // Restore shortcuts and widgets to original
                    ShortcutDeliveryService.updateBatchShortcutsSilently(context, restoreUpdates)
                    SingleIconWidgetProvider.updateAllWidgets(context)

                    _history.value = undoStore.getHistory()
                }
            }
            _isLoading.value = false
            onComplete(revertedDesc)
        }
    }

    fun exportPackZip(targetFile: File, onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val items = _apps.value.mapNotNull { app ->
                    val bmp = app.styledBitmap ?: return@mapNotNull null
                    val cleanDrawableName = "icon_" + app.info.packageName.replace(".", "_")
                    IconPackExporter.ExportItem(
                        packageName = app.info.packageName,
                        appLabel = app.info.appLabel,
                        iconDrawableName = cleanDrawableName,
                        iconBitmap = bmp
                    )
                }
                IconPackExporter.exportPackToZip(context, _activeStyle.value.name, items, targetFile)
            }
            onComplete()
        }
    }

    fun exportToGalleryForOneUi(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            var count = 0
            withContext(Dispatchers.IO) {
                val items = _apps.value.mapNotNull { app ->
                    val bmp = app.styledBitmap ?: return@mapNotNull null
                    val cleanDrawableName = "icon_" + app.info.packageName.replace(".", "_")
                    IconPackExporter.ExportItem(
                        packageName = app.info.packageName,
                        appLabel = app.info.appLabel,
                        iconDrawableName = cleanDrawableName,
                        iconBitmap = bmp
                    )
                }
                count = IconPackExporter.exportToGalleryForOneUiThemePark(
                    context,
                    _activeStyle.value.name.replace(" ", "_"),
                    items
                )
            }
            onComplete(count)
        }
    }

    fun addRule(rule: AppRule) {
        val updated = _rules.value + rule
        ruleStore.saveRules(updated)
        _rules.value = updated
    }

    fun deleteRule(ruleId: String) {
        val updated = _rules.value.filter { it.id != ruleId }
        ruleStore.saveRules(updated)
        _rules.value = updated
    }
}
