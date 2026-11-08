package com.anacatalina.androidiconsadmin.ui.screens

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
import androidx.compose.ui.unit.dp
import com.anacatalina.androidiconsadmin.core.styling.ContainerShape
import com.anacatalina.androidiconsadmin.core.styling.ExtractionMode
import com.anacatalina.androidiconsadmin.core.styling.IconStyle
import com.anacatalina.androidiconsadmin.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleStudioScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val styles by viewModel.styles.collectAsState()
    val activeStyle by viewModel.activeStyle.collectAsState()
    val apps by viewModel.apps.collectAsState()

    var currentShape by remember(activeStyle) { mutableStateOf(activeStyle.container.shape) }
    var currentBgColor by remember(activeStyle) { mutableStateOf(activeStyle.container.backgroundColor) }
    var currentPadding by remember(activeStyle) { mutableFloatStateOf(activeStyle.container.padding.toFloat()) }

    var currentSat by remember(activeStyle) { mutableFloatStateOf(activeStyle.treatment.saturation.toFloat()) }
    var currentBri by remember(activeStyle) { mutableFloatStateOf(activeStyle.treatment.brightness.toFloat()) }
    var currentCon by remember(activeStyle) { mutableFloatStateOf(activeStyle.treatment.contrast.toFloat()) }
    var currentMode by remember(activeStyle) { mutableStateOf(activeStyle.extractionMode) }

    val previewApps = remember(apps) { apps.take(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estudio de Estilos") },
                actions = {
                    Button(onClick = {
                        val updated = activeStyle.copy(
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
                        viewModel.saveActiveStyle(updated)
                        Toast.makeText(context, "Estilo '${updated.name}' guardado", Toast.LENGTH_SHORT).show()
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
                        onClick = { viewModel.selectActiveStyle(style) },
                        label = { Text(style.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Vista Previa en Vivo", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (item in previewApps) {
                    val bmp = item.styledBitmap ?: item.originalBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.info.appLabel,
                            modifier = Modifier.size(54.dp)
                        )
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
