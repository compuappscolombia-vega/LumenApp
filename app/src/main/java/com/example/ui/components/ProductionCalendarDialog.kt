package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProductionProject
import com.example.data.model.SequenceVersionStatus
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver
import java.util.Calendar
import java.util.Locale

data class ProductionMilestone(
    val id: String,
    val title: String,
    val subtitle: String,
    val dateMillis: Long,
    val type: MilestoneType,
    val sequenceId: String? = null
)

enum class MilestoneType {
    PROJECT_CREATION,
    SEQUENCE_DRAFT,
    SEQUENCE_FINAL,
    SHOOTING_DAY
}

@Composable
fun ProductionCalendarDialog(
    project: ProductionProject?,
    onDismiss: () -> Unit,
    onSelectSequence: (String) -> Unit = {}
) {
    val calendarInstance = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(calendarInstance.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendarInstance.get(Calendar.MONTH)) } // 0-11
    var selectedDay by remember { mutableIntStateOf(calendarInstance.get(Calendar.DAY_OF_MONTH)) }

    val milestones = remember(project) {
        val list = mutableListOf<ProductionMilestone>()
        if (project != null) {
            if (project.createdAtMillis > 0L) {
                list.add(
                    ProductionMilestone(
                        id = "proj_create",
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
                            title = "Guion / Borrador: ${seq.title}",
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
                            title = "Aprobación Versión Final: ${seq.title}",
                            subtitle = "Bloqueada para postproducción y exportación PDF",
                            dateMillis = seq.updatedAtMillis,
                            type = MilestoneType.SEQUENCE_FINAL,
                            sequenceId = seq.id
                        )
                    )
                }
            }
        }
        list
    }

    val daysInMonthList = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth, 1, 0, 0, 0)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
        val list = mutableListOf<Int?>()
        // Padding for leading empty cells
        repeat(startDayOfWeek - 1) { list.add(null) }
        for (day in 1..maxDays) {
            list.add(day)
        }
        list
    }

    val monthNames = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    val selectedDayMilestones = remember(selectedYear, selectedMonth, selectedDay, milestones) {
        milestones.filter { milestone ->
            val cal = Calendar.getInstance().apply { timeInMillis = milestone.dateMillis }
            cal.get(Calendar.YEAR) == selectedYear &&
                cal.get(Calendar.MONTH) == selectedMonth &&
                cal.get(Calendar.DAY_OF_MONTH) == selectedDay
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCard,
        titleContentColor = AmberGold,
        textContentColor = IvoryPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = AmberGold)
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
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month Header Navigation
                Surface(
                    color = StudioDeepPanel,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            },
                            modifier = Modifier.testTag("calendar_prev_month")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = IvoryPrimary)
                        }
                        Text(
                            text = "${monthNames[selectedMonth]} $selectedYear",
                            style = MaterialTheme.typography.titleMedium,
                            color = AmberGold,
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
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = IvoryPrimary)
                        }
                    }
                }

                // Days of week header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("D", "L", "M", "X", "J", "V", "S").forEach { dow ->
                        Text(
                            text = dow,
                            style = MaterialTheme.typography.labelSmall,
                            color = MutedStone,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.size(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Calendar Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .testTag("production_calendar_grid"),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(daysInMonthList) { dayOpt ->
                        if (dayOpt == null) {
                            Box(modifier = Modifier.size(36.dp))
                        } else {
                            val hasMilestone = milestones.any { milestone ->
                                val cal = Calendar.getInstance().apply { timeInMillis = milestone.dateMillis }
                                cal.get(Calendar.YEAR) == selectedYear &&
                                    cal.get(Calendar.MONTH) == selectedMonth &&
                                    cal.get(Calendar.DAY_OF_MONTH) == dayOpt
                            }
                            val isSelected = dayOpt == selectedDay

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> AmberGold
                                            hasMilestone -> TealContainer
                                            else -> StudioDeepPanel
                                        }
                                    )
                                    .border(
                                        width = if (hasMilestone && !isSelected) 1.dp else 0.dp,
                                        color = TealAtmos,
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
                                        color = if (isSelected) ObsidianBg else IvoryPrimary,
                                        fontWeight = if (isSelected || hasMilestone) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (hasMilestone && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(AmberGold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Milestones & Deadlines list for selected day
                Text(
                    text = "Hitos y secuencias para el $selectedDay de ${monthNames[selectedMonth]} $selectedYear",
                    style = MaterialTheme.typography.labelMedium,
                    color = WarmSilver,
                    fontWeight = FontWeight.SemiBold
                )

                if (selectedDayMilestones.isEmpty()) {
                    Surface(
                        color = StudioDeepPanel,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    ) {
                        Text(
                            text = "Sin hitos o secuencias programadas para esta fecha.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedStone,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(selectedDayMilestones, key = { it.id }) { milestone ->
                            Surface(
                                color = StudioDeepPanel,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                                    .let { if (milestone.sequenceId != null) it.clickable { onSelectSequence(milestone.sequenceId); onDismiss() } else it }
                                    .testTag("calendar_milestone_${milestone.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = when (milestone.type) {
                                            MilestoneType.PROJECT_CREATION -> Icons.Rounded.Event
                                            MilestoneType.SEQUENCE_FINAL -> Icons.Rounded.CheckCircle
                                            else -> Icons.Rounded.Movie
                                        },
                                        contentDescription = null,
                                        tint = if (milestone.type == MilestoneType.SEQUENCE_FINAL) TealAtmos else AmberGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = milestone.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IvoryPrimary,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = milestone.subtitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MutedStone,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                Text(stringResource(R.string.action_close), color = IvoryPrimary)
            }
        }
    )
}
