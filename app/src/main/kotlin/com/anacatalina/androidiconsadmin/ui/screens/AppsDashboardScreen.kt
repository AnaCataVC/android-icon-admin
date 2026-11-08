package com.anacatalina.androidiconsadmin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anacatalina.androidiconsadmin.ui.AppItemUiState
import com.anacatalina.androidiconsadmin.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsDashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeStyle by viewModel.activeStyle.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps else apps.filter {
            it.info.appLabel.contains(searchQuery, ignoreCase = true) ||
                    it.info.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val selectedCount = remember(apps) { apps.count { it.isSelected } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Apps Instaladas (${apps.size})") },
                actions = {
                    IconButton(onClick = { viewModel.loadInstalledApps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar apps")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        viewModel.selectAllApps(selectedCount < apps.size)
                    }) {
                        Text(if (selectedCount < apps.size) "Seleccionar Todo" else "Deseleccionar")
                    }

                    Button(
                        onClick = {
                            viewModel.applyStyleToSelected { count ->
                                Toast.makeText(context, "Estilo aplicado a $count apps", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isLoading && apps.isNotEmpty()
                    ) {
                        Text(if (selectedCount > 0) "Aplicar a ($selectedCount)" else "Aplicar a Todas")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar aplicación o paquete...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            if (isLoading && apps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    contentPadding = PaddingValues(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredApps, key = { it.info.packageName }) { appItem ->
                        AppIconCard(
                            item = appItem,
                            onToggleSelect = { viewModel.toggleAppSelection(appItem.info.packageName) },
                            onPinShortcut = {
                                viewModel.pinAppShortcut(appItem) { success ->
                                    val msg = if (success) "Acceso directo solicitado para ${appItem.info.appLabel}"
                                    else "El launcher no soporta pinned shortcuts"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppIconCard(
    item: AppItemUiState,
    onToggleSelect: () -> Unit,
    onPinShortcut: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(6.dp)
            .fillMaxWidth()
            .clickable(onClick = onToggleSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                val previewBitmap = item.styledBitmap ?: item.originalBitmap
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = item.info.appLabel,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(4.dp)
                    )
                } else {
                    Box(modifier = Modifier.size(64.dp))
                }

                Icon(
                    imageVector = if (item.isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.info.appLabel,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = onPinShortcut,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = "Fijar en Home",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
