package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SystemErrorEntry
import com.example.data.model.SystemErrorSeverity
import com.example.data.model.SystemErrorStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemErrorCenterDialog(
    errors: List<SystemErrorEntry>,
    lastRepairSummary: String?,
    onDismiss: () -> Unit,
    onRepairError: (errorId: String) -> Unit,
    onRunFullRepair: () -> Unit,
    onMarkRepaired: (errorId: String) -> Unit,
    onClearRepaired: () -> Unit,
    onExportReport: (List<SystemErrorEntry>) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var statusFilter by remember { mutableStateOf("all") }

    val openCount = remember(errors) { errors.count { it.status == SystemErrorStatus.OPEN } }
    val repairedCount = remember(errors) { errors.count { it.status == SystemErrorStatus.REPAIRED } }
    val filteredErrors = remember(errors, statusFilter) {
        when (statusFilter) {
            "open" -> errors.filter { it.status == SystemErrorStatus.OPEN }
            "repaired" -> errors.filter { it.status == SystemErrorStatus.REPAIRED }
            else -> errors
        }
    }
    val dateFmt = remember {
        SimpleDateFormat("dd/MM HH:mm:ss", Locale.forLanguageTag("es-ES"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(0.95f)
            .testTag("system_error_center_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Registro de Errores y Reparación",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primary
                    )
                    Text(
                        text = "Diagnóstico automático, causa probable y auto-reparación en 1 toque",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("errors_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Cerrar registro de errores",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                        // Health Summary + Repair CTA Card
                        Surface(
                            color = colors.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (openCount > 0) colors.errorContainer else colors.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("errors_open_count_badge")
                                    ) {
                                        Text(
                                            text = if (openCount > 0) "$openCount pendiente(s)" else "Sistema saludable · 0 pendientes",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (openCount > 0) colors.onErrorContainer else colors.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                    Surface(
                                        color = colors.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = "$repairedCount reparado(s)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = colors.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                if (!lastRepairSummary.isNullOrBlank()) {
                                    Surface(
                                        color = colors.secondaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("errors_last_repair_summary")
                                    ) {
                                        Text(
                                            text = "Última reparación: $lastRepairSummary",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSecondaryContainer,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = onRunFullRepair,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.primary,
                                        contentColor = colors.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("run_full_repair_button")
                                ) {
                                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(8.dp))
                                    Text("Ejecutar auto-reparación integral", fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = onClearRepaired,
                                        enabled = repairedCount > 0,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("clear_repaired_errors_button")
                                    ) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Limpiar reparados", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { onExportReport(errors) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("export_error_report_button")
                                    ) {
                                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Exportar reporte", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    }
                                }
                            }
                        }

                        // Filter Chips
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to "Todos (${errors.size})",
                                "open" to "Pendientes ($openCount)",
                                "repaired" to "Reparados ($repairedCount)"
                            ).forEach { (id, label) ->
                                FilterChip(
                                    selected = statusFilter == id,
                                    onClick = { statusFilter = id },
                                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer,
                                        containerColor = colors.surfaceVariant,
                                        labelColor = colors.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("errors_filter_$id")
                                )
                            }
                        }

                        if (filteredErrors.isEmpty()) {
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.outline, RoundedCornerShape(14.dp))
                                    .testTag("errors_empty_state")
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VerifiedUser,
                                        contentDescription = null,
                                        tint = colors.secondary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "Sistema saludable · Sin incidencias en este filtro",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = "Cualquier fallo de Gemini AI, Firestore, PDF, dictado por voz o validación queda guardado aquí automáticamente con su reparación recomendada.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            filteredErrors.forEach { err ->
                                val isRepaired = err.status == SystemErrorStatus.REPAIRED
                                val severityContainer = when {
                                    isRepaired -> colors.secondaryContainer
                                    err.severity == SystemErrorSeverity.CRITICAL -> colors.errorContainer
                                    err.severity == SystemErrorSeverity.WARNING -> colors.primaryContainer
                                    else -> colors.surface
                                }
                                val severityText = when {
                                    isRepaired -> colors.onSecondaryContainer
                                    err.severity == SystemErrorSeverity.CRITICAL -> colors.onErrorContainer
                                    err.severity == SystemErrorSeverity.WARNING -> colors.onPrimaryContainer
                                    else -> colors.onSurface
                                }

                                Surface(
                                    color = colors.surfaceVariant,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (isRepaired) colors.secondary.copy(alpha = 0.5f) else colors.outline,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .testTag("system_error_card_${err.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(color = severityContainer, shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    text = if (isRepaired) "Reparado" else err.severity.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = severityText,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                color = colors.surface,
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.border(1.dp, colors.outline, RoundedCornerShape(6.dp))
                                            ) {
                                                Text(
                                                    text = err.source.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = dateFmt.format(Date(err.timestampMillis)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isRepaired) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                                contentDescription = null,
                                                tint = if (isRepaired) colors.secondary else colors.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = err.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.onSurface,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = err.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurface
                                        )

                                        if (err.rootCauseHint.isNotBlank()) {
                                            Text(
                                                text = "Causa probable: ${err.rootCauseHint}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.onSurfaceVariant
                                            )
                                        }
                                        if (err.suggestedFix.isNotBlank() && !isRepaired) {
                                            Surface(
                                                color = colors.surface,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, colors.outline, RoundedCornerShape(8.dp))
                                            ) {
                                                Text(
                                                    text = "Solución: ${err.suggestedFix}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.primary,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                        if (isRepaired && err.repairNote.isNotBlank()) {
                                            Surface(
                                                color = colors.secondaryContainer,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Reparación aplicada: ${err.repairNote}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.onSecondaryContainer,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }

                                        if (!isRepaired) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { onRepairError(err.id) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = colors.primary,
                                                        contentColor = colors.onPrimary
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                        .testTag("repair_error_button_${err.id}")
                                                ) {
                                                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text(err.repairAction.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                                }
                                                TextButton(
                                                    onClick = { onMarkRepaired(err.id) },
                                                    modifier = Modifier.testTag("mark_repaired_button_${err.id}")
                                                ) {
                                                    Text("Marcar reparado", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = colors.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
