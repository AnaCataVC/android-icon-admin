package com.anacatalina.androidiconsadmin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anacatalina.androidiconsadmin.core.rules.AppRule
import com.anacatalina.androidiconsadmin.core.rules.RuleCondition
import com.anacatalina.androidiconsadmin.ui.MainViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val rules by viewModel.rules.collectAsState()
    val styles by viewModel.styles.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reglas Automáticas") },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Regla")
                    }
                }
            )
        }
    ) { padding ->
        if (rules.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay reglas configuradas.\nCrea una para auto-asignar estilos por nombre o paquete.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rule.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Si ${rule.targetField} ${rule.condition.name.lowercase()} '${rule.pattern}' -> Estilo: ${rule.targetStyleId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar Regla")
                            }
                        }
                    }
                }
            }
        }

        if (showDialog) {
            AddRuleDialog(
                styles = styles.map { it.id to it.name },
                onDismiss = { showDialog = false },
                onAdd = { newRule ->
                    viewModel.addRule(newRule)
                    showDialog = false
                    Toast.makeText(context, "Regla '${newRule.name}' guardada", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun AddRuleDialog(
    styles: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onAdd: (AppRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var targetField by remember { mutableStateOf("packageName") }
    var condition by remember { mutableStateOf(RuleCondition.CONTAINS) }
    var selectedStyleId by remember { mutableStateOf(styles.firstOrNull()?.first ?: "style-soft-lavender") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Regla de Estilo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre de la regla (ej. Google Apps)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text("Patrón o texto a buscar") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && pattern.isNotBlank()) {
                        val rule = AppRule(
                            id = "rule_${UUID.randomUUID().toString().take(8)}",
                            name = name,
                            targetField = targetField,
                            condition = condition,
                            pattern = pattern,
                            targetStyleId = selectedStyleId
                        )
                        onAdd(rule)
                    }
                },
                enabled = name.isNotBlank() && pattern.isNotBlank()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
