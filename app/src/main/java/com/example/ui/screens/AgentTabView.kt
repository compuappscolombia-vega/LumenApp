package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.util.AgentLogic
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberGold
import com.example.ui.theme.IvoryPrimary
import com.example.ui.theme.MutedStone
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDeepPanel
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TealAtmos
import com.example.ui.theme.TealContainer
import com.example.ui.theme.WarmSilver

@Composable
fun AgentTabView(
    project: ProductionProject,
    activeSequence: ProductionSequence?,
    messages: List<AgentMessage>,
    isBusy: Boolean,
    onSendMessage: (AgentRoleFocus, String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFocus by remember(project.id) { mutableStateOf(AgentRoleFocus.COUNCIL) }
    var draftMessage by remember(project.id) { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Clear action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = AmberContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "Agente Tripartito LumenReel",
                            tint = AmberGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Agente Tripartito Autónomo",
                        style = MaterialTheme.typography.titleMedium,
                        color = IvoryPrimary
                    )
                    Text(
                        text = activeSequence?.let { "Trabajando sobre: ${it.title}" }
                            ?: "Preproducción · Biblia y estructura base",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedStone
                    )
                }
            }
            if (messages.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    enabled = !isBusy,
                    modifier = Modifier.testTag("agent_clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Reiniciar historial",
                        tint = MutedStone,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text("Limpiar", color = MutedStone, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Role Focus Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AgentLogic.AGENT_ROLE_PROFILES.forEach { profile ->
                val selected = selectedFocus == profile.id
                FilterChip(
                    selected = selected,
                    onClick = { selectedFocus = profile.id },
                    label = {
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberContainer,
                        selectedLabelColor = AmberGold,
                        containerColor = StudioCard,
                        labelColor = WarmSilver
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = StudioBorder,
                        selectedBorderColor = AmberGold.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.testTag("agent_focus_${profile.id.wireValue}")
                )
            }
        }

        val activeProfile = AgentLogic.AGENT_ROLE_PROFILES.first { it.id == selectedFocus }
        Text(
            text = "${activeProfile.badge} — ${activeProfile.description}",
            style = MaterialTheme.typography.bodySmall,
            color = MutedStone
        )

        // Messages List or Quick Commands
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = AmberGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Comandos rápidos de producción",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AmberGold
                                )
                            }
                            AgentLogic.AGENT_QUICK_COMMANDS.forEach { cmd ->
                                Surface(
                                    color = StudioDeepPanel,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isBusy) {
                                            selectedFocus = cmd.focus
                                            onSendMessage(cmd.focus, cmd.prompt)
                                        }
                                        .testTag("agent_quick_${cmd.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = cmd.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = IvoryPrimary
                                        )
                                        Text(
                                            text = cmd.prompt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedStone
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(messages, key = { it.id.ifBlank { "${it.createdAtMillis}_${it.role}_${it.content.hashCode()}" } }) { msg ->
                    AgentMessageBubble(msg = msg)
                }
            }

            if (isBusy) {
                item {
                    Surface(
                        color = StudioCard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = AmberGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "El consejo creativo está evaluando y actualizando el expediente…",
                                style = MaterialTheme.typography.bodySmall,
                                color = WarmSilver
                            )
                        }
                    }
                }
            }
        }

        // Composer Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draftMessage,
                onValueChange = { draftMessage = it },
                placeholder = {
                    Text(
                        text = "Pide cambios de dirección, guion, VFX/SFX o Biblia…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedStone
                    )
                },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AmberGold,
                    unfocusedBorderColor = StudioBorder,
                    focusedContainerColor = StudioCard,
                    unfocusedContainerColor = StudioCard,
                    focusedTextColor = IvoryPrimary,
                    unfocusedTextColor = IvoryPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("agent_message_input")
            )
            Surface(
                color = if (draftMessage.isNotBlank() && !isBusy) AmberGold else StudioSurfaceElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                IconButton(
                    onClick = {
                        val text = draftMessage.trim()
                        if (text.isNotEmpty() && !isBusy) {
                            draftMessage = ""
                            onSendMessage(selectedFocus, text)
                        }
                    },
                    enabled = draftMessage.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("agent_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Enviar instrucción al agente",
                        tint = if (draftMessage.isNotBlank() && !isBusy) ObsidianBg else MutedStone
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(msg: AgentMessage) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) AmberContainer else StudioCard,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) AmberGold.copy(alpha = 0.45f) else StudioBorder,
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "TÚ" else "LUMENREEL · ${msg.focus.wireValue.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) AmberGold else TealAtmos,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IvoryPrimary
                )

                if (msg.actions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        msg.actions.forEach { act ->
                            Surface(
                                color = TealContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, TealAtmos.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Build,
                                        contentDescription = null,
                                        tint = TealAtmos,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "[${act.affectedArea.uppercase()}] ${act.summary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IvoryPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
