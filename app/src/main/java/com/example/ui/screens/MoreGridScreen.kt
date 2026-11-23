package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SaveSlotSummary
import com.example.model.GameSpeed
import com.example.model.GameState
import com.example.model.NavDestination
import com.example.ui.components.OledBadge
import com.example.ui.components.OledCard
import com.example.ui.components.OledOutlinedButton
import com.example.ui.components.OledPrimaryButton
import com.example.ui.components.WorldOrderHubHeader
import com.example.ui.components.formatUsd
import com.example.ui.theme.*

private data class MoreTile(
    val title: String,
    val dest: NavDestination,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun MoreGridScreen(
    state: GameState,
    onNavigate: (NavDestination) -> Unit
) {
    val unreadAlerts = state.notifications.count { !it.isRead }
    // Exact 4x4 grid specified:
    // Row 1: Economy, Organizations, Conflict, Personal
    // Row 2: Knowledge, University, Ranking, Supranational organizations
    // Row 3: Messages, Awards, Market, Tutorial
    // Row 4: Rules, Society, Administration, Settings
    val tiles = listOf(
        MoreTile("Economy", NavDestination.ECONOMY, Icons.Filled.AttachMoney, AccentGreen),
        MoreTile("Organizations", NavDestination.ORGANIZATIONS, Icons.Filled.AccountBalance, AccentGold),
        MoreTile("Conflict", NavDestination.CONFLICT, Icons.Filled.LocalFireDepartment, AccentRed),
        MoreTile("Personal", NavDestination.PERSONAL, Icons.Filled.Badge, AccentCyan),

        MoreTile("Knowledge", NavDestination.KNOWLEDGE, Icons.Filled.Psychology, AccentPurple),
        MoreTile("University", NavDestination.UNIVERSITY, Icons.Filled.School, AccentCyan),
        MoreTile("Ranking", NavDestination.RANKING, Icons.Filled.Leaderboard, AccentGold),
        MoreTile("Supranational organizations", NavDestination.SUPRANATIONAL, Icons.Filled.Public, AccentCyan),

        MoreTile("Messages", NavDestination.INBOX, Icons.Filled.Email, AccentGold),
        MoreTile("Awards", NavDestination.AWARDS, Icons.Filled.EmojiEvents, AccentGold),
        MoreTile("Market", NavDestination.MARKET, Icons.Filled.Storefront, AccentBitcoin),
        MoreTile("Tutorial", NavDestination.TUTORIAL, Icons.Filled.HelpOutline, AccentGreen),

        MoreTile("Rules", NavDestination.RULES, Icons.Filled.Gavel, AccentPurple),
        MoreTile("Society", NavDestination.SOCIETY, Icons.Filled.Groups, AccentGreen),
        MoreTile("Administration", NavDestination.ADMINISTRATION, Icons.Filled.AdminPanelSettings, AccentCyan),
        MoreTile("Settings", NavDestination.SETTINGS, Icons.Filled.Settings, TextSecondary)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Full-width top green-bordered Notifications button
        OledCard(
            borderColor = AccentGreen,
            containerColor = OledCard,
            onClick = { onNavigate(NavDestination.NOTIFICATIONS) },
            modifier = Modifier.testTag("more_notifications_btn")
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, contentDescription = "Notifications", tint = AccentGreen, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Notifications", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    Text(
                        state.notifications.firstOrNull()?.title ?: "No unread dispatches",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDescription,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (unreadAlerts > 0) OledBadge("$unreadAlerts NEW", AccentGreen)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Exact 4x4 Navigation Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(tiles, key = { it.title }) { tile ->
                Column(
                    modifier = Modifier
                        .testTag("more_tile_${tile.dest.name.lowercase()}")
                        .aspectRatio(0.92f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OledCard)
                        .border(1.dp, OledBorder, RoundedCornerShape(10.dp))
                        .clickable { onNavigate(tile.dest) }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tile.icon, contentDescription = tile.title, tint = tile.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp),
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: GameState,
    saveSlots: List<SaveSlotSummary>,
    onSpeedSelected: (GameSpeed) -> Unit,
    onSaveToSlot: (Int) -> Unit,
    onLoadFromSlot: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit,
    onOpenNewCampaign: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WorldOrderHubHeader(
            title = "Settings",
            quote = "Simulation Speed, 5-Slot Offline Save Manager & Campaign Controls"
        )

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                OledCard {
                    Text("Simulation Clock Speed", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameSpeed.entries.forEach { spd ->
                            FilterChip(
                                selected = state.speed == spd,
                                onClick = { onSpeedSelected(spd) },
                                label = { Text(spd.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            item { Text("5-Slot Local Offline Save / Load", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
            items(saveSlots, key = { it.slotId }) { slot ->
                OledCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (slot.isEmpty) "Slot ${slot.slotId} • Empty" else "Slot ${slot.slotId}: ${slot.campaignName}",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = if (slot.isEmpty) "Tap Save to archive campaign locally" else "${slot.countryName} • ${slot.dateFormatted} • Lv.${slot.playerLevel} (${formatUsd(slot.netWorthUsd)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDescription
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OledPrimaryButton("Save", { onSaveToSlot(slot.slotId) }, Modifier.weight(1f), accent = AccentGreen)
                        OledOutlinedButton("Load", { onLoadFromSlot(slot.slotId) }, Modifier.weight(1f), enabled = !slot.isEmpty, accent = AccentCyan)
                        OledOutlinedButton("Delete", { onDeleteSlot(slot.slotId) }, Modifier.weight(1f), enabled = !slot.isEmpty, accent = AccentRed)
                    }
                }
            }

            item {
                OledOutlinedButton(
                    text = "Start New Campaign (Career / Sandbox)",
                    onClick = onOpenNewCampaign,
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentGold
                )
            }
        }
    }
}

