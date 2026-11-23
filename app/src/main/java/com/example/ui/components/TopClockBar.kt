package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SaveSlotSummary
import com.example.model.GameSpeed
import com.example.model.GameState
import com.example.ui.theme.*

@Composable
fun TopClockBar(
    state: GameState,
    saveSlots: List<SaveSlotSummary>,
    onSpeedSelected: (GameSpeed) -> Unit,
    onSaveToSlot: (Int) -> Unit,
    onLoadFromSlot: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit,
    onOpenNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    val home = state.countries.firstOrNull { it.iso == state.player.countryIso }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(OledBlack)
            .statusBarsPadding()
            .border(width = 1.dp, color = OledBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = "Simulation Clock",
                tint = AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = state.date.formatted(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Speed Toggles: ||, 1x, 2x, 5x, 10x
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GameSpeed.entries.forEach { spd ->
                    val active = state.speed == spd
                    val col = when {
                        active && spd == GameSpeed.PAUSED -> AccentRed
                        active -> AccentCyan
                        else -> TextSecondary
                    }
                    Box(
                        modifier = Modifier
                            .testTag("speed_${spd.name}")
                            .clip(RoundedCornerShape(5.dp))
                            .background(if (active) col.copy(alpha = 0.18f) else OledCard)
                            .border(1.dp, if (active) col else OledBorder, RoundedCornerShape(5.dp))
                            .clickable { onSpeedSelected(spd) }
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = spd.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = col
                        )
                    }
                }
            }

            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .testTag("save_load_button")
                    .clip(RoundedCornerShape(6.dp))
                    .background(OledElevated)
                    .border(1.dp, OledBorderHighlight, RoundedCornerShape(6.dp))
                    .clickable { showSaveDialog = true }
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = "5-Slot Save Manager",
                    tint = AccentGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Compact Ticker Bar: Cash, Local FX, Araucitas, Nation & Level
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "USD ${formatUsd(state.player.cashUsd)}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AccentGreen
            )
            if (home != null) {
                Text(
                    text = "${home.currencySymbol}${String.format("%,.0f", state.player.cashLocal)} ${home.currencyCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Text(
                text = "₿ ${String.format("%.4f", state.player.bitcoinBalance)}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentBitcoin
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Diamond, contentDescription = "Araucitas", tint = AccentPurple, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(3.dp))
                Text(
                    text = "${state.player.araucitas} Araucitas",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPurple
                )
            }
            Text(
                text = "Lv.${state.player.level} • ${home?.iso ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = AccentCyan
            )
        }
    }

    if (showSaveDialog) {
        SaveLoadDialog(
            slots = saveSlots,
            onDismiss = { showSaveDialog = false },
            onSave = {
                onSaveToSlot(it)
            },
            onLoad = {
                onLoadFromSlot(it)
                showSaveDialog = false
            },
            onDelete = onDeleteSlot,
            onNewCampaign = {
                showSaveDialog = false
                onOpenNewGame()
            }
        )
    }
}

@Composable
fun SaveLoadDialog(
    slots: List<SaveSlotSummary>,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit,
    onLoad: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onNewCampaign: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text("Offline 5-Slot Campaign Manager", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { slot ->
                    OledCard(containerColor = OledElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (slot.isEmpty) "Slot ${slot.slotId} • Empty" else "Slot ${slot.slotId}: ${slot.campaignName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (slot.isEmpty) TextMuted else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!slot.isEmpty) {
                                    Text(
                                        text = "${slot.countryName} • Lv.${slot.playerLevel} • ${formatUsd(slot.netWorthUsd)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentCyan
                                    )
                                    Text(
                                        text = slot.dateFormatted,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onSave(slot.slotId) }) {
                                    Text("Save", color = AccentGreen, fontSize = 11.sp)
                                }
                                if (!slot.isEmpty) {
                                    TextButton(onClick = { onLoad(slot.slotId) }) {
                                        Text("Load", color = AccentCyan, fontSize = 11.sp)
                                    }
                                    TextButton(onClick = { onDelete(slot.slotId) }) {
                                        Text("Del", color = AccentRed, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewCampaign) {
                Text("New Campaign", color = AccentGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}
