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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.rounded.ContentPaste
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AgentMessage
import com.example.data.model.AgentRoleFocus
import com.example.data.model.ProductionProject
import com.example.data.model.ProductionSequence
import com.example.data.util.AgentLogic
import com.example.ui.components.readClipboardText

@Composable
fun AgentTabView(
    project: ProductionProject,
    activeSequence: ProductionSequence?,
    messages: List<AgentMessage>,
    selectedFocus: AgentRoleFocus,
    draftMessage: String,
    isBusy: Boolean,
    onFocusChange: (AgentRoleFocus) -> Unit,
    onDraftChange: (String) -> Unit,
    onSendMessage: (AgentRoleFocus, String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
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
                    color = colors.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.SmartToy,
                            contentDescription = "Agente Tripartito LumenReel",
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Agente Tripartito Autónomo",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface
                    )
                    Text(
                        text = activeSequence?.let { "Trabajando sobre: ${it.title}" }
                            ?: "Preproducción · Biblia y estructura base",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f)
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
                        tint = colors.onSurfaceVariant.copy(alpha = 0.78f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        "Limpiar",
                        color = colors.onSurfaceVariant.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall
                    )
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
                    onClick = { onFocusChange(profile.id) },
                    label = {
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primaryContainer,
                        selectedLabelColor = colors.onPrimaryContainer,
                        containerColor = colors.surface,
                        labelColor = colors.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = colors.outline,
                        selectedBorderColor = colors.primary.copy(alpha = 0.65f)
                    ),
                    modifier = Modifier.testTag("agent_focus_${profile.id.wireValue}")
                )
            }
        }

        val activeProfile = AgentLogic.AGENT_ROLE_PROFILES.first { it.id == selectedFocus }
        Text(
            text = "${activeProfile.badge} — ${activeProfile.description}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant.copy(alpha = 0.78f)
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
                        color = colors.surface,
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Comandos rápidos de producción",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.primary
                                )
                            }
                            AgentLogic.AGENT_QUICK_COMMANDS.forEach { cmd ->
                                Surface(
                                    color = colors.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, colors.outline, RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isBusy) {
                                            onFocusChange(cmd.focus)
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
                                            color = colors.onSurface
                                        )
                                        Text(
                                            text = cmd.prompt,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant.copy(alpha = 0.78f)
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
                        color = colors.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, colors.outline, RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "El consejo creativo está evaluando y actualizando el expediente…",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
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
                onValueChange = onDraftChange,
                placeholder = {
                    Text(
                        text = "Pide cambios de dirección, guion, VFX/SFX o Biblia…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant.copy(alpha = 0.72f)
                    )
                },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.outline,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.onSurface,
                    unfocusedTextColor = colors.onSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("agent_message_input")
            )
            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(52.dp)
                    .border(1.dp, colors.outline, RoundedCornerShape(12.dp))
            ) {
                IconButton(
                    onClick = {
                        val clip = readClipboardText(context) ?: return@IconButton
                        val next = if (draftMessage.isBlank()) clip else "${draftMessage.trim()} $clip"
                        onDraftChange(next)
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("agent_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentPaste,
                        contentDescription = "Pegar texto del portapapeles",
                        tint = colors.onSurfaceVariant
                    )
                }
            }
            Surface(
                color = if (draftMessage.isNotBlank() && !isBusy) colors.primary else colors.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                IconButton(
                    onClick = {
                        val text = draftMessage.trim()
                        if (text.isNotEmpty() && !isBusy) {
                            onDraftChange("")
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
                        tint = if (draftMessage.isNotBlank() && !isBusy) colors.onPrimary else colors.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentMessageBubble(msg: AgentMessage) {
    val isUser = msg.role == "user"
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) colors.primaryContainer else colors.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .widthIn(max = 580.dp)
                .border(
                    width = 1.dp,
                    color = if (isUser) colors.primary.copy(alpha = 0.45f) else colors.outline,
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
                        color = if (isUser) colors.onPrimaryContainer else colors.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) colors.onPrimaryContainer else colors.onSurface
                )

                if (msg.actions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        msg.actions.forEach { act ->
                            Surface(
                                color = colors.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, colors.secondary.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Build,
                                        contentDescription = null,
                                        tint = colors.secondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "[${act.affectedArea.uppercase()}] ${act.summary}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.onSecondaryContainer
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
