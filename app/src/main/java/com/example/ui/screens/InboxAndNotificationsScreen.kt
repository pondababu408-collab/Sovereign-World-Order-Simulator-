package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.GameState
import com.example.model.MailType
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun InboxScreen(
    state: GameState,
    onRespondMail: (String, Boolean) -> Unit,
    onSendDirectMessage: (String, String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0: Official Mail, 1: Direct Messages (DMs)
    val vipNpcs = remember(state.npcs) {
        state.npcs.filter { it.isHeadOfState || it.officeHeld != null }.take(12).ifEmpty { state.npcs.take(12) }
    }
    var selectedNpcId by remember(vipNpcs) { mutableStateOf(vipNpcs.firstOrNull()?.id ?: "") }
    var messageDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WorldOrderHubHeader(
            title = "Messages",
            quote = "\"Diplomacy is the art of telling people to go to hell in such a way that they ask for directions.\" - Churchill"
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Official Mail (${state.officialMails.size})") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Direct Messages") })
        }

        Spacer(Modifier.height(8.dp))

        if (tab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.officialMails.isEmpty()) {
                    item {
                        OledCard {
                            Text("Your Official Mailbox is empty.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                }

                items(state.officialMails, key = { it.id }) { mail ->
                    val borderCol = if (mail.status == "PENDING" && mail.type != MailType.SYSTEM_DISPATCH) AccentGold.copy(alpha = 0.6f) else OledBorder
                    OledCard(borderColor = borderCol) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(mail.senderName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OledBadge(mail.status, when (mail.status) { "ACCEPTED" -> AccentGreen; "DECLINED" -> AccentRed; else -> AccentGold })
                        }
                        Text("${mail.senderRole} • ${mail.timestampStr}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        Spacer(Modifier.height(6.dp))
                        Text(mail.subject, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(mail.body, style = MaterialTheme.typography.bodySmall, color = TextDescription)

                        if (mail.status == "PENDING" && mail.type != MailType.SYSTEM_DISPATCH) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OledPrimaryButton("Accept Offer", { onRespondMail(mail.id, true) }, Modifier.weight(1f), accent = AccentGreen)
                                OledOutlinedButton("Decline", { onRespondMail(mail.id, false) }, Modifier.weight(1f), accent = AccentRed)
                            }
                        }
                    }
                }
            }
        } else {
            // Direct Messaging Room with AI NPCs
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                vipNpcs.forEach { npc ->
                    FilterChip(
                        selected = selectedNpcId == npc.id,
                        onClick = { selectedNpcId = npc.id },
                        label = { Text("@${npc.handle} (${npc.countryIso})", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            val activeNpc = state.npcs.firstOrNull { it.id == selectedNpcId }
            val thread = remember(state.directMessages, selectedNpcId) {
                state.directMessages.filter { it.npcId == selectedNpcId }
            }

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activeNpc != null) {
                    item {
                        OledCard(containerColor = OledElevated) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                ExecutivePortrait(activeNpc.avatarSeed, activeNpc.name, size = 36.dp)
                                Column {
                                    Text("${activeNpc.name} (@${activeNpc.handle})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("${activeNpc.countryIso} • ${activeNpc.ambition.label}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                }
                            }
                        }
                    }
                }
                if (thread.isEmpty()) {
                    item {
                        OledCard {
                            Text("Start an encrypted channel with @${activeNpc?.handle ?: "npc"} regarding coalition deals, B2B liquidity, or diplomacy.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        }
                    }
                }
                items(thread, key = { it.id }) { dm ->
                    OledCard(
                        borderColor = if (dm.fromPlayer) AccentGreen.copy(alpha = 0.5f) else OledBorder
                    ) {
                        Text(if (dm.fromPlayer) "You (@${state.player.handle})" else "${dm.npcName} (@${dm.npcHandle})", style = MaterialTheme.typography.labelSmall, color = if (dm.fromPlayer) AccentGreen else AccentCyan)
                        Spacer(Modifier.height(2.dp))
                        Text(dm.text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        Text(dm.timestampStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                    placeholder = { Text("Dispatch encrypted message...", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (selectedNpcId.isNotBlank() && messageDraft.isNotBlank()) {
                            onSendDirectMessage(selectedNpcId, messageDraft)
                            messageDraft = ""
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send DM", tint = AccentGreen)
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    state: GameState,
    onMarkAllRead: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Alerts & Dispatch Stream",
                subtitle = "Real-time economic, legislative & geopolitical alerts",
                icon = Icons.Filled.Notifications,
                accent = AccentCyan,
                trailing = {
                    OledOutlinedButton("Mark Read", onMarkAllRead)
                }
            )
        }

        items(state.notifications, key = { it.id }) { alert ->
            OledCard(borderColor = if (!alert.isRead) AccentCyan.copy(alpha = 0.5f) else OledBorder) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(alert.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    OledBadge(alert.category, AccentCyan)
                }
                Spacer(Modifier.height(4.dp))
                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(alert.timestampStr, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

