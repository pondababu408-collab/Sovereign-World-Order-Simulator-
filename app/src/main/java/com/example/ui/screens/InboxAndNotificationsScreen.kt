package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onRespondMail: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Official Mailbox (Correo)",
                subtitle = "Parliamentary seats, Ministry offers, Party co-founding & Noble Titles",
                icon = Icons.Filled.Email,
                accent = AccentGold
            )
        }

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
                Text(mail.body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)

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
