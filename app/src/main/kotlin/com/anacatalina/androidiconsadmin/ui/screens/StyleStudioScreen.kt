package com.anacatalina.androidiconsadmin.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anacatalina.androidiconsadmin.core.styling.ContainerShape
import com.anacatalina.androidiconsadmin.core.styling.ExtractionMode
import com.anacatalina.androidiconsadmin.core.styling.IconStyle
import com.anacatalina.androidiconsadmin.core.styling.IconStyleRenderer
import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas
import com.anacatalina.androidiconsadmin.platform.BitmapCanvasBridge
import com.anacatalina.androidiconsadmin.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleStudioScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val styles by viewModel.styles.collectAsState()
    val activeStyle by viewModel.activeStyle.collectAsState()
    val apps by viewModel.apps.collectAsState()

    var currentShape by remember(activeStyle.id) { mutableStateOf(activeStyle.container.shape) }
    var currentBgColor by remember(activeStyle.id) { mutableStateOf(activeStyle.container.backgroundColor) }
    var currentPadding by remember(activeStyle.id) { mutableFloatStateOf(activeStyle.container.padding.toFloat()) }

    var currentSat by remember(activeStyle.id) { mutableFloatStateOf(activeStyle.treatment.saturation.toFloat()) }
    var currentBri by remember(activeStyle.id) { mutableFloatStateOf(activeStyle.treatment.brightness.toFloat()) }
    var currentCon by remember(activeStyle.id) { mutableFloatStateOf(activeStyle.treatment.contrast.toFloat()) }
    var currentMode by remember(activeStyle.id) { mutableStateOf(activeStyle.extractionMode) }

    val currentDraftStyle = remember(
        activeStyle.id,
        activeStyle.name,
        currentShape,
        currentBgColor,
        currentPadding,
        currentSat,
        currentBri,
        currentCon,
        currentMode
    ) {
        activeStyle.copy(
            container = activeStyle.container.copy(
                shape = currentShape,
                backgroundColor = currentBgColor,
                padding = currentPadding.toDouble()
            ),
            treatment = activeStyle.treatment.copy(
                saturation = currentSat.toDouble(),
                brightness = currentBri.toDouble(),
                contrast = currentCon.toDouble()
            ),
            extractionMode = currentMode
        )
    }

    val previewItems by produceState<List<Pair<String, Bitmap>>>(
        initialValue = emptyList(),
        currentDraftStyle,
        apps
    ) {
        value = withContext(Dispatchers.Default) {
            val targetApps = apps.take(5)
            val renderedList = targetApps.mapNotNull { item ->
                val canvas = viewModel.snapshotStore.getSnapshot(item.info.packageName, currentDraftStyle.extractionMode.name)
                    ?: viewModel.snapshotStore.getSnapshot(item.info.packageName)
                    ?: item.originalBitmap?.let { BitmapCanvasBridge.toRgbaCanvas(it) }
                val styledCanvas = canvas?.let { IconStyleRenderer.render(it, currentDraftStyle, 256) }
                val bmp = styledCanvas?.let { BitmapCanvasBridge.toBitmap(it) }
                bmp?.let { item.info.appLabel to it }
            }
            if (renderedList.isNotEmpty()) {
                renderedList
            } else {
                generateSamplePreviewIcons(currentDraftStyle)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudio de Estilos") },
                actions = {
                    Button(onClick = {
                        viewModel.saveActiveStyle(currentDraftStyle)
                        Toast.makeText(context, "Estilo '${currentDraftStyle.name}' guardado", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Guardar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Presets Disponibles", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(styles) { style ->
                    val isSelected = style.id == activeStyle.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectActiveStyle(style)
                            currentShape = style.container.shape
                            currentBgColor = style.container.backgroundColor
                            currentPadding = style.container.padding.toFloat()
                            currentSat = style.treatment.saturation.toFloat()
                            currentBri = style.treatment.brightness.toFloat()
                            currentCon = style.treatment.contrast.toFloat()
                            currentMode = style.extractionMode
                        },
                        label = { Text(style.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Vista Previa en Vivo", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (previewItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .height(64.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else {
                        for ((label, bmp) in previewItems) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = label,
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Forma del Contenedor", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ContainerShape.SQUIRCLE to "Squircle",
                    ContainerShape.ONE_UI_SQUIRCLE to "One UI 8.5",
                    ContainerShape.ROUNDED_SQUARE to "Cuadrado",
                    ContainerShape.CIRCLE to "Círculo",
                    ContainerShape.TEARDROP to "Teardrop"
                ).forEach { (shape, label) ->
                    FilterChip(
                        selected = currentShape == shape,
                        onClick = { currentShape = shape },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Color de Fondo: $currentBgColor", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "#E6DBFA" to "Lavender",
                    "#EDE7F6" to "One UI Pastel",
                    "#1E222B" to "Midnight",
                    "#D5E2D4" to "Sage",
                    "#F7DDD4" to "Terracotta",
                    "#E8EAED" to "Material Grey"
                ).forEach { (hex, _) ->
                    val colorInt = android.graphics.Color.parseColor(hex)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                            .border(
                                width = if (currentBgColor.equals(hex, true)) 3.dp else 1.dp,
                                color = if (currentBgColor.equals(hex, true)) MaterialTheme.colorScheme.primary else Color.Gray,
                                shape = CircleShape
                            )
                            .clickable { currentBgColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Padding / Margen: ${(currentPadding * 100).toInt()}%")
            Slider(
                value = currentPadding,
                onValueChange = { currentPadding = it },
                valueRange = 0.05f..0.35f
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Saturación (Rec.709): ${(currentSat * 100).toInt()}%")
            Slider(
                value = currentSat,
                onValueChange = { currentSat = it },
                valueRange = -1.0f..1.0f
            )

            Text("Brillo: ${(currentBri * 100).toInt()}%")
            Slider(
                value = currentBri,
                onValueChange = { currentBri = it },
                valueRange = -0.5f..0.5f
            )

            Text("Contraste: ${(currentCon * 100).toInt()}%")
            Slider(
                value = currentCon,
                onValueChange = { currentCon = it },
                valueRange = -0.5f..0.5f
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Modo de Extracción de Iconos", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ExtractionMode.SMART_FOREGROUND to "Smart Foreground",
                    ExtractionMode.FULL_COMPOSITE to "Compuesto Completo",
                    ExtractionMode.MONOCHROME_PREFERRED to "Monocromo (Android 13+)"
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = currentMode == mode,
                        onClick = { currentMode = mode },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

private fun generateSamplePreviewIcons(style: IconStyle): List<Pair<String, Bitmap>> {
    val samples = listOf(
        "Teléfono" to createPhoneIconCanvas(),
        "Mensajes" to createChatIconCanvas(),
        "Cámara" to createCameraIconCanvas(),
        "Música" to createMusicIconCanvas(),
        "Ajustes" to createSettingsIconCanvas()
    )
    return samples.map { (label, canvas) ->
        val styled = IconStyleRenderer.render(canvas, style, 256)
        label to BitmapCanvasBridge.toBitmap(styled)
    }
}

private fun createPhoneIconCanvas(): RgbaCanvas {
    val canvas = RgbaCanvas(256, 256)
    for (y in 60 until 196) {
        for (x in 90 until 166) {
            val inTopEar = y in 60 until 95 && x in 90 until 150
            val inStem = y in 95 until 160 && x in 100 until 130
            val inBottomMic = y in 160 until 196 && x in 90 until 150
            if (inTopEar || inStem || inBottomMic) {
                canvas.setPixel(x, y, 255, 255, 255, 255)
            }
        }
    }
    return canvas
}

private fun createChatIconCanvas(): RgbaCanvas {
    val canvas = RgbaCanvas(256, 256)
    for (y in 70 until 175) {
        for (x in 60 until 196) {
            val inBubble = (x in 75 until 181) || (y in 85 until 160)
            val inTail = (x in 70 until 100 && y in 165 until 195 && (x - 70) <= (195 - y))
            if (inBubble || inTail) {
                canvas.setPixel(x, y, 255, 255, 255, 255)
            }
        }
    }
    return canvas
}

private fun createCameraIconCanvas(): RgbaCanvas {
    val canvas = RgbaCanvas(256, 256)
    for (y in 90 until 175) {
        for (x in 65 until 191) {
            canvas.setPixel(x, y, 255, 255, 255, 255)
        }
    }
    for (y in 75 until 90) {
        for (x in 105 until 151) {
            canvas.setPixel(x, y, 255, 255, 255, 255)
        }
    }
    val cx = 128.0
    val cy = 132.0
    val r = 24.0
    for (y in 108..156) {
        for (x in 104..152) {
            val d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (d2 <= r * r) {
                canvas.setPixel(x, y, 0, 0, 0, 0)
            }
        }
    }
    return canvas
}

private fun createMusicIconCanvas(): RgbaCanvas {
    val canvas = RgbaCanvas(256, 256)
    for (y in 80 until 150) {
        for (x in 135 until 150) {
            canvas.setPixel(x, y, 255, 255, 255, 255)
        }
    }
    for (y in 80 until 105) {
        for (x in 150 until 180) {
            canvas.setPixel(x, y, 255, 255, 255, 255)
        }
    }
    val cx = 120.0
    val cy = 155.0
    val r = 22.0
    for (y in 133..177) {
        for (x in 98..142) {
            val d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (d2 <= r * r) {
                canvas.setPixel(x, y, 255, 255, 255, 255)
            }
        }
    }
    return canvas
}

private fun createSettingsIconCanvas(): RgbaCanvas {
    val canvas = RgbaCanvas(256, 256)
    val cx = 128.0
    val cy = 128.0
    val rOuter = 50.0
    val rInner = 24.0
    for (y in 70..186) {
        for (x in 70..186) {
            val d2 = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            val inRing = d2 in (rInner * rInner)..(rOuter * rOuter)
            val inCross = (x in 116..140 && y in 66..190) || (y in 116..140 && x in 66..190)
            if (inRing || (inCross && d2 <= (rOuter + 14.0) * (rOuter + 14.0) && d2 >= rInner * rInner)) {
                canvas.setPixel(x, y, 255, 255, 255, 255)
            }
        }
    }
    return canvas
}
