package com.anacatalina.androidiconsadmin.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.anacatalina.androidiconsadmin.ui.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryAndUndoScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val history by viewModel.history.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Exportación, Undo y Límites") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Acciones Rápidas de Entrega", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                viewModel.exportToGalleryForOneUi { count ->
                                    Toast.makeText(
                                        context,
                                        "Exportados $count iconos a Pictures/AndroidIconsAdmin/ para Theme Park",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PhotoAlbum, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar para Samsung One UI 8.5 Theme Park")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                val zipFile = File(exportDir, "AndroidIconsAdmin_Pack.zip")
                                viewModel.exportPackZip(zipFile) {
                                    Toast.makeText(
                                        context,
                                        "ZIP generado en ${zipFile.name} (${zipFile.length() / 1024} KB)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar Paquete ZIP (appfilter.xml)")
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Historial de Lotes (Undo Transaccional)", style = MaterialTheme.typography.titleMedium)
                            IconButton(
                                onClick = {
                                    viewModel.revertLatestBatch { desc ->
                                        val msg = desc?.let { "Lote revertido: $it" } ?: "No hay lotes para revertir"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = history.isNotEmpty() && !isLoading
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Deshacer último lote")
                            }
                        }

                        if (history.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No se han aplicado cambios aún.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            val sdf = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.getDefault()) }
                            history.take(5).forEach { record ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "• ${record.description} (${sdf.format(Date(record.timestampMillis))})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Límites del Sistema Android (Sin Root)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "1. Un app normal NO puede modificar APKs instalados ni sobrescribir el cajón de apps de Pixel Launcher o One UI Home.\n" +
                                    "2. Pinned Shortcuts: Android 8+ muestra una pequeña insignia del app creador.\n" +
                                    "3. Widgets 1x1: ¡Cero insignias! Se actualizan en lote silenciosamente al cambiar estilos o revertir.\n" +
                                    "4. One UI 8.5 Theme Park / Launchers de Terceros: Usa la exportación para tematizar todo el sistema.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
