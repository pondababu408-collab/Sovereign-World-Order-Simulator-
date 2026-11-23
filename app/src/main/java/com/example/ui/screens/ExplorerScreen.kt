package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ExplorerScreen(
    state: GameState,
    initialTab: Int = 0,
    onInspectNpc: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(initialTab) } // 0: Citizens Search, 1: Highest Level, 2: Richest Net Worth, 3: Top Politicians, 4: Largest Companies
    val tabs = listOf("Citizens (${state.npcs.size})", "Level", "Net Worth", "Politicians", "Companies")

    val filteredNpcs = remember(searchQuery, state.npcs) {
        val base = if (searchQuery.isBlank()) state.npcs else state.npcs.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.handle.contains(searchQuery, ignoreCase = true) ||
                it.countryIso.contains(searchQuery, ignoreCase = true) ||
                it.traits.any { t -> t.contains(searchQuery, ignoreCase = true) }
        }
        base.take(80)
    }

    val byLevel = remember(state.npcs) { state.npcs.sortedByDescending { it.level }.take(40) }
    val byWealth = remember(state.npcs) { state.npcs.sortedByDescending { it.netWorthUsd }.take(40) }
    val topPoliticians = remember(state.npcs) {
        state.npcs.filter { it.isHeadOfState || it.officeHeld != null || it.isParliamentMember }
            .sortedByDescending { (if (it.isHeadOfState) 100 else 0) + (if (it.officeHeld != null) 40 else 0) + it.level }
            .take(40)
    }
    val topCompanies = remember(state.companies) {
        state.companies.sortedByDescending { it.valuationUsd }.take(40)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OledSectionHeader(
            title = "Global Explorer & Rankings",
            subtitle = "${state.npcs.size} Autonomous AI Citizens • ${state.companies.size} Corporations",
            icon = Icons.Filled.Leaderboard,
            accent = AccentCyan
        )

        Spacer(Modifier.height(6.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it; if (selectedTab != 0) selectedTab = 0 },
            placeholder = { Text("Search NPC name, @handle, country ISO, or trait...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AccentCyan.copy(alpha = 0.16f) else OledCard)
                        .border(1.dp, if (active) AccentCyan else OledBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedTab = idx }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
                        color = if (active) AccentCyan else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedTab) {
                0 -> items(filteredNpcs, key = { it.id }) { npc ->
                    NpcListRow(npc = npc, rank = null, onClick = { onInspectNpc(npc.id) })
                }
                1 -> itemsIndexed(byLevel, key = { _, n -> n.id }) { idx, npc ->
                    NpcListRow(npc = npc, rank = idx + 1, onClick = { onInspectNpc(npc.id) })
                }
                2 -> itemsIndexed(byWealth, key = { _, n -> n.id }) { idx, npc ->
                    NpcListRow(npc = npc, rank = idx + 1, onClick = { onInspectNpc(npc.id) })
                }
                3 -> itemsIndexed(topPoliticians, key = { _, n -> n.id }) { idx, npc ->
                    NpcListRow(npc = npc, rank = idx + 1, onClick = { onInspectNpc(npc.id) })
                }
                4 -> itemsIndexed(topCompanies, key = { _, c -> c.id }) { idx, comp ->
                    OledCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelMedium, color = AccentGold, modifier = Modifier.width(30.dp))
                            CorporateLogo(ticker = comp.ticker, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(comp.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${comp.countryIso} • ${comp.type.displayName} • ${comp.employeesCount} Staff", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatUsd(comp.valuationUsd), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AccentGreen)
                                if (comp.isStateOwnedEnterprise) OledBadge("SOE", AccentGold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NpcListRow(
    npc: NpcCharacter,
    rank: Int?,
    onClick: () -> Unit
) {
    val tierColor = when (npc.traitTier) {
        TraitTier.HIGH -> AccentGold
        TraitTier.MID -> AccentCyan
        TraitTier.LOW -> TextSecondary
    }
    OledCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rank != null) {
                Text("#$rank", style = MaterialTheme.typography.labelMedium, color = AccentGold, modifier = Modifier.width(28.dp))
            }
            ExecutivePortrait(seed = npc.avatarSeed, name = npc.name, accent = tierColor, size = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(npc.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    OledBadge(npc.traitTier.name, tierColor)
                    if (npc.isHeadOfState) OledBadge("HOS", AccentGold)
                    else npc.officeHeld?.let { OledBadge(it.code, AccentPurple) }
                }
                Text("@${npc.handle} • ${npc.countryIso} • ${npc.ambition.label}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(npc.lastAction, style = MaterialTheme.typography.labelSmall, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Lv.${npc.level}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
                Text(formatUsd(npc.netWorthUsd), style = MaterialTheme.typography.labelSmall, color = AccentGreen)
            }
        }
    }
}

@Composable
fun NpcProfileModal(
    npc: NpcCharacter,
    state: GameState,
    isFollowing: Boolean,
    onToggleFollow: () -> Unit,
    onDismiss: () -> Unit
) {
    val country = state.countries.firstOrNull { it.iso == npc.countryIso }
    val party = state.parties.firstOrNull { it.id == npc.partyId }
    val tierColor = when (npc.traitTier) {
        TraitTier.HIGH -> AccentGold
        TraitTier.MID -> AccentCyan
        TraitTier.LOW -> TextSecondary
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = OledCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExecutivePortrait(seed = npc.avatarSeed, name = npc.name, accent = tierColor, size = 48.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(npc.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("@${npc.handle} • ${country?.name ?: npc.countryIso}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OledBadge(npc.traitTier.label, tierColor)
                    OledBadge("Lv.${npc.level}", AccentCyan)
                }
                Text("Ambition: ${npc.ambition.label}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Traits: ${npc.traits.joinToString(" • ")}", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                if (party != null) {
                    Text("Party: ${party.name} [${party.acronym}]", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                npc.officeHeld?.let {
                    Text("Office Held: ${it.title}", style = MaterialTheme.typography.bodySmall, color = AccentPurple)
                }
                OledCard(containerColor = OledElevated) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatMini("INT", npc.intelligence)
                        StatMini("STR", npc.strength)
                        StatMini("END", npc.endurance)
                        StatMini("EFF", npc.efficiency)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Liquid Cash: ${formatUsd(npc.cashUsd)} • Net Worth: ${formatUsd(npc.netWorthUsd)}", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                }
                Text("Latest AI Action: ${npc.lastAction}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onToggleFollow) {
                Text(if (isFollowing) "Unfollow" else "Follow", color = AccentCyan, fontSize = 13.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontSize = 13.sp)
            }
        }
    )
}

@Composable
private fun StatMini(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text("$value", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
    }
}
