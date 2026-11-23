package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun ConflictScreen(
    state: GameState,
    onLaunchCovertOp: (String, CovertOpType) -> Unit,
    onLaunchConflict: (String, String) -> Unit
) {
    val home = state.countries.first { it.iso == state.player.countryIso }
    val foreignTargets = remember(state.countries, state.player.countryIso) {
        state.countries.filter { it.iso != state.player.countryIso }.take(25)
    }
    var targetIso by remember { mutableStateOf(foreignTargets.firstOrNull()?.iso ?: "US") }
    val targetCountry = state.countries.firstOrNull { it.iso == targetIso } ?: foreignTargets.first()

    val hasIntel = SimulationEngine.hasPortfolioAccess(state, MinistryTag.INTELLIGENCE)
    val hasDefense = SimulationEngine.hasPortfolioAccess(state, MinistryTag.DEFENSE)
    val hasSecurity = SimulationEngine.hasPortfolioAccess(state, MinistryTag.SECURITY)

    val intelHolder = state.npcs.firstOrNull { it.id == home.ministerAssignments[MinistryTag.INTELLIGENCE.name] }?.name ?: "You"
    val defHolder = state.npcs.firstOrNull { it.id == home.ministerAssignments[MinistryTag.DEFENSE.name] }?.name ?: "You"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Conflict, Military & Espionage",
                subtitle = "${home.intelAgencyName} & Armed Forces Command",
                icon = Icons.Filled.Shield,
                accent = AccentRed
            )
        }

        item {
            PortfolioLockBanner(MinistryTag.INTELLIGENCE, hasIntel, intelHolder)
            Spacer(Modifier.height(6.dp))
            PortfolioLockBanner(MinistryTag.DEFENSE, hasDefense, defHolder)
        }

        item {
            OledCard {
                Text("Select Target Country", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    foreignTargets.forEach { c ->
                        FilterChip(
                            selected = targetIso == c.iso,
                            onClick = { targetIso = c.iso },
                            label = { Text("${c.iso} (${c.bilateralRelation})", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("${targetCountry.name} • Stability ${targetCountry.stability}% • Spy Network ${targetCountry.spyNetworkStrength}%", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
            }
        }

        // Covert Intelligence Operations
        item {
            OledCard {
                OledSectionHeader("Covert Intelligence & Sabotage", "Strict Ministry Tag Enforcement", Icons.Filled.Security, AccentPurple)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CovertOpType.entries.forEach { opType ->
                        val unlocked = when (opType.requiredMinistry) {
                            MinistryTag.INTELLIGENCE -> hasIntel
                            MinistryTag.DEFENSE -> hasDefense
                            MinistryTag.SECURITY -> hasSecurity
                            else -> false
                        }
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(opType.label, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("Requires [${opType.requiredMinistry.code}]", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                            OledPrimaryButton(
                                text = "Execute",
                                enabled = unlocked,
                                onClick = { onLaunchCovertOp(targetIso, opType) },
                                accent = AccentPurple
                            )
                        }
                    }
                }
            }
        }

        // Military Escalation
        item {
            OledCard {
                Text("Strategic & Military Operations ([DEFENSE])", style = MaterialTheme.typography.titleSmall, color = AccentRed)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OledOutlinedButton("Trade Sanctions", { onLaunchConflict(targetIso, "Trade War") }, Modifier.weight(1f), hasDefense, AccentGold)
                    OledOutlinedButton("Border Skirmish", { onLaunchConflict(targetIso, "Border Clash") }, Modifier.weight(1f), hasDefense, AccentRed)
                }
            }
        }

        if (state.covertOps.isNotEmpty()) {
            item { Text("Recent Covert Dispatches", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
            items(state.covertOps.take(10), key = { it.id }) { op ->
                OledCard {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${op.type.label} → ${op.targetCountryIso}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        OledBadge(op.status, if (op.status == "SUCCESS") AccentGreen else AccentRed)
                    }
                    Text("Success Odds: ${op.successChancePct}% • Day ${op.launchedDay}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
        }

        if (state.conflicts.isNotEmpty()) {
            item { Text("Active Conflicts", style = MaterialTheme.typography.titleSmall, color = AccentRed) }
            items(state.conflicts, key = { it.id }) { war ->
                OledCard(borderColor = AccentRed.copy(alpha = 0.5f)) {
                    Text("${war.aggressorIso} vs ${war.defenderIso} • ${war.intensity}", style = MaterialTheme.typography.titleSmall, color = AccentRed)
                    Text("Frontline: ${war.frontlineProvinceId} • Strength ${war.aggressorStrength} vs ${war.defenderStrength}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}
