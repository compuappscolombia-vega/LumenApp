package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CalendarMilestoneType
import com.example.data.model.ProductionMilestoneItem
import com.example.data.model.ProductionProject
import com.example.data.model.SequenceVersionStatus
import java.util.Calendar

data class ProductionMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateMillis: Long,
    val type: MilestoneType,
    val sequenceId: String? = null,
    val customMilestoneId: String? = null,
    val completed: Boolean = false
)

enum class MilestoneType {
    PROJECT_CREATION,
    SEQUENCE_DRAFT,
    SEQUENCE_FINAL,
    SHOOTING_DAY,
    DEADLINE,
    REVIEW,
    PREMIERE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductionCalendarDialog(
    project: ProductionProject?,
    milestones: List<ProductionMilestoneItem> = emptyList(),
    onDismiss: () -> Unit,
    onSelectSequence: (String) -> Unit = {},
    onAddMilestone: (
        title: String,
        notes: String,
        dateMillis: Long,
        type: CalendarMilestoneType,
        sequenceId: String?
    ) -> Unit = { _, _, _, _, _ -> },
    onToggleMilestone: (milestoneId: String, completed: Boolean) -> Unit = { _, _ -> },
    onDeleteMilestone: (milestoneId: String) -> Unit = {}
) {
    val calendarInstance = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(calendarInstance.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendarInstance.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendarInstance.get(Calendar.DAY_OF_MONTH)) }

    var showCreateForm by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf(CalendarMilestoneType.SHOOTING_DAY) }

    val combinedMilestones = remember(project, milestones) {
        val list = mutableListOf<ProductionMilestone>()
        if (project != null) {
            if (project.createdAtMillis > 0L) {
                list.add(
                    ProductionMilestone(
                        id = "proj_create_${project.id}",
                        title = "Inicio de Producción: ${project.title}",
                        subtitle = project.genreStyle,
                        dateMillis = project.createdAtMillis,
                        type = MilestoneType.PROJECT_CREATION
                    )
                )
            }
            project.sequences.forEach { seq ->
                if (seq.createdAtMillis > 0L) {
                    list.add(
                        ProductionMilestone(
                            id = "seq_create_${seq.id}",
                            title = "Guion / Escenas: ${seq.title}",
                            subtitle = "${seq.screenplay.size} escenas · ${seq.shotList.size} planos",
                            dateMillis = seq.createdAtMillis,
                            type = MilestoneType.SEQUENCE_DRAFT,
                            sequenceId = seq.id
                        )
                    )
                }
                if (seq.versionStatus == SequenceVersionStatus.FINAL && seq.updatedAtMillis > 0L) {
                    list.add(
                        ProductionMilestone(
                            id = "seq_final_${seq.id}",
                            title = "Versión Final: ${seq.title}",
                            subtitle = "Aprobada para rodaje y exportación PDF",
                            dateMillis = seq.updatedAtMillis,
                            type = MilestoneType.SEQUENCE_FINAL,
                            sequenceId = seq.id
                        )
                    )
                }
            }
        }
        milestones.forEach { item ->
            val mappedType = when (item.resolvedType()) {
                CalendarMilestoneType.SHOOTING_DAY -> MilestoneType.SHOOTING_DAY
                CalendarMilestoneType.DEADLINE -> MilestoneType.DEADLINE
                CalendarMilestoneType.REVIEW -> MilestoneType.REVIEW
                CalendarMilestoneType.PREMIERE -> MilestoneType.PREMIERE
            }
            val typeLabel = item.resolvedType().displayLabel
            list.add(
                ProductionMilestone(
                    id = "custom_${item.id}",
                    title = item.title,
                    subtitle = item.notes.ifBlank { typeLabel },
                    dateMillis = item.dateMillis,
                    type = mappedType,
                    sequenceId = item.sequenceId,
                    customMilestoneId = item.id,
                    completed = item.completed
                )
            )
        }
        list
    }

    val daysInMonthList = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val list = mutableListOf<Int?>()
        repeat(startDayOfWeek - 1) { list.add(null) }
        for (day in 1..maxDays) {
            list.add(day)
        }
        list
    }

    val monthNames = remember {
        listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        )
    }

    val selectedDayMilestones = remember(selectedYear, selectedMonth, selectedDay, combinedMilestones) {
        combinedMilestones.filter { milestone ->
            val cal = Calendar.getInstance().apply { timeInMillis = milestone.dateMillis }
            cal.get(Calendar.YEAR) == selectedYear &&
                cal.get(Calendar.MONTH) == selectedMonth &&
                cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.primary,
        textContentColor = colors.onSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.primary)
                Text(
                    text = if (project != null) "Calendario: ${project.title}" else "Calendario de Producción",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (selectedMonth == 0) {
                                    selectedMonth = 11
                                    selectedYear -= 1
                                } else {
                                    selectedMonth -= 1
                                }
                                selectedDay = selectedDay.coerceIn(1, 28)
                            },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Mes anterior",
                                tint = colors.onSurface
                            )
                        }
                        Text(
                            text = "${monthNames[selectedMonth]} $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = {
                                if (selectedMonth == 11) {
                                    selectedMonth = 0
                                    selectedYear += 1
                                } else {
                                    selectedMonth += 1
                                }
                                selectedDay = selectedDay.coerceIn(1, 28)
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = "Mes siguiente",
                                tint = colors.onSurface
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("D", "L", "M", "X", "J", "V", "S").forEach { dow ->
                        Text(
                            text = dow,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                val weeks = remember(daysInMonthList) {
                    daysInMonthList.chunked(7).map { week ->
                        if (week.size < 7) week + List(7 - week.size) { null } else week
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("production_calendar_grid"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    weeks.forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { dayOpt ->
                                if (dayOpt == null) {
                                    Box(modifier = Modifier.size(34.dp))
                                } else {
                                    val hasMilestone = combinedMilestones.any { milestone ->
                                        val cal = Calendar.getInstance().apply { timeInMillis = milestone.dateMillis }
                                        cal.get(Calendar.YEAR) == selectedYear &&
                                            cal.get(Calendar.MONTH) == selectedMonth &&
                                            cal.get(Calendar.DAY_OF_MONTH) == dayOpt
                                    }
                                    val isSelected = dayOpt == selectedDay

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> colors.primary
                                                    hasMilestone -> colors.secondaryContainer
                                                    else -> colors.surfaceVariant
                                                }
                                            )
                                            .border(
                                                width = if (hasMilestone && !isSelected) 1.dp else 0.dp,
                                                color = colors.secondary,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedDay = dayOpt }
                                            .testTag("calendar_day_$dayOpt"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayOpt.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = when {
                                                    isSelected -> colors.onPrimary
                                                    hasMilestone -> colors.onSecondaryContainer
                                                    else -> colors.onSurface
                                                },
                                                fontWeight = if (isSelected || hasMilestone) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasMilestone && !isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedDay de ${monthNames[selectedMonth]} $selectedYear",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (project != null) {
                        OutlinedButton(
                            onClick = { showCreateForm = !showCreateForm },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("calendar_add_milestone_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (showCreateForm) "Ocultar" else "Añadir hito",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Milestone list for selected day continues below; creation form opens in dedicated dialog.

                if (selectedDayMilestones.isEmpty()) {
                    Surface(
                        color = colors.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "Sin hitos ni escenas programadas para esta fecha.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedDayMilestones.forEach { milestone ->
                            Surface(
                                color = colors.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = if (milestone.completed) colors.secondary.copy(alpha = 0.5f) else colors.outline,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .let { mod ->
                                        if (milestone.sequenceId != null && milestone.customMilestoneId == null) {
                                            mod.clickable {
                                                onSelectSequence(milestone.sequenceId)
                                                onDismiss()
                                            }
                                        } else {
                                            mod
                                        }
                                    }
                                    .testTag("calendar_milestone_${milestone.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (milestone.customMilestoneId != null) {
                                        IconButton(
                                            onClick = {
                                                onToggleMilestone(
                                                    milestone.customMilestoneId,
                                                    !milestone.completed
                                                )
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("calendar_toggle_ms_${milestone.customMilestoneId}")
                                        ) {
                                            Icon(
                                                imageVector = if (milestone.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                                contentDescription = if (milestone.completed) "Marcar como pendiente" else "Marcar como completado",
                                                tint = if (milestone.completed) colors.secondary else colors.primary
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = when (milestone.type) {
                                                MilestoneType.PROJECT_CREATION -> Icons.Rounded.Event
                                                MilestoneType.SEQUENCE_FINAL -> Icons.Rounded.CheckCircle
                                                MilestoneType.SHOOTING_DAY -> Icons.Rounded.Videocam
                                                MilestoneType.DEADLINE -> Icons.Rounded.Schedule
                                                MilestoneType.REVIEW -> Icons.Rounded.Flag
                                                else -> Icons.Rounded.Movie
                                            },
                                            contentDescription = null,
                                            tint = if (milestone.type == MilestoneType.SEQUENCE_FINAL) colors.secondary else colors.primary,
                                            modifier = Modifier.padding(start = 6.dp).size(20.dp)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .let { mod ->
                                                if (milestone.sequenceId != null) {
                                                    mod.clickable {
                                                        onSelectSequence(milestone.sequenceId)
                                                        onDismiss()
                                                    }
                                                } else mod
                                            }
                                    ) {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (milestone.completed) colors.onSurfaceVariant else colors.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (milestone.completed) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = milestone.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (milestone.customMilestoneId != null) {
                                        IconButton(
                                            onClick = { onDeleteMilestone(milestone.customMilestoneId) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("calendar_delete_ms_${milestone.customMilestoneId}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Eliminar hito",
                                                tint = colors.onSurfaceVariant
                                            )
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
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_calendar_dialog")
            ) {
                Text(stringResource(R.string.action_close), color = colors.onSurface)
            }
        }
    )

    if (showCreateForm && project != null) {
        AlertDialog(
            onDismissRequest = { showCreateForm = false },
            containerColor = colors.surface,
            titleContentColor = colors.primary,
            textContentColor = colors.onSurface,
            title = {
                Text("Nuevo Hito · $selectedDay de ${monthNames[selectedMonth]}")
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CalendarMilestoneType.entries.forEach { t ->
                            FilterChip(
                                selected = newType == t,
                                onClick = { newType = t },
                                label = {
                                    Text(t.displayLabel, style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer,
                                    containerColor = colors.surfaceVariant,
                                    labelColor = colors.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("calendar_type_${t.wireValue}")
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Título del hito") },
                        placeholder = { Text("Ej. Rodaje nocturno en muelle", color = colors.onSurfaceVariant) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_milestone_title_input")
                    )
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Escenas / notas (opcional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline,
                            focusedTextColor = colors.onSurface,
                            unfocusedTextColor = colors.onSurface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_milestone_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanTitle = newTitle.trim()
                        if (cleanTitle.isNotEmpty()) {
                            val targetCal = Calendar.getInstance().apply {
                                set(selectedYear, selectedMonth, selectedDay, 12, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            onAddMilestone(
                                cleanTitle,
                                newNotes.trim(),
                                targetCal.timeInMillis,
                                newType,
                                project.sequences.firstOrNull()?.id
                            )
                            newTitle = ""
                            newNotes = ""
                            showCreateForm = false
                        }
                    },
                    enabled = newTitle.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    modifier = Modifier.testTag("calendar_save_milestone_button")
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateForm = false }) {
                    Text(stringResource(R.string.action_cancel), color = colors.onSurface)
                }
            }
        )
    }
}
