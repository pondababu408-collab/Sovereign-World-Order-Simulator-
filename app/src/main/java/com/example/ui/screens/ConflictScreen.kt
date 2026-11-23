package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class ConflictSubRoute(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
) {
    ACTIVE_WARS("Active Wars & Frontlines", "Regional clashes, sanctions & escalation.", Icons.Filled.LocalFireDepartment, AccentRed),
    ARMED_FORCES("Armed Forces", "Divisions, air squadrons, naval fleets & readiness.", Icons.Filled.Shield, AccentCyan),
    MILITARY_PROCUREMENT("Military Procurement", "Acquire rifles, ammo, drones, artillery & tanks.", Icons.Filled.Inventory, AccentGold),
    INSURGENCIES("Insurgencies & Coups", "Counter-insurgency, domestic stability & sweeps.", Icons.Filled.Warning, AccentAmber),
    COVERT_INTEL("Covert Intelligence", "Espionage networks, currency sabotage & black ops.", Icons.Filled.Security, AccentPurple),
    PMCS("Private Military (PMCs)", "Contract autonomous tactical battalions worldwide.", Icons.Filled.Groups, AccentGreen)
}

@Composable
fun ConflictScreen(
    state: GameState,
    onLaunchCovertOp: (String, CovertOpType) -> Unit,
    onLaunchConflict: (String, String) -> Unit,
    onBuyHardware: (String, Int, Double) -> Unit,
    onHirePmc: () -> Unit
) {
    var activeSub by remember { mutableStateOf<ConflictSubRoute?>(null) }
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
    val secHolder = state.npcs.firstOrNull { it.id == home.ministerAssignments[MinistryTag.SECURITY.name] }?.name ?: "You"

    BackHandler(enabled = activeSub != null) {
        activeSub = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (activeSub == null) {
            WorldOrderHubHeader(
                title = "Conflict",
                quote = "\"War is the continuation of policy with other means.\" - Carl von Clausewitz"
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ConflictSubRoute.entries, key = { it.name }) { route ->
                    HubMenuCard(
                        title = route.title,
                        description = route.description,
                        icon = route.icon,
                        accent = route.accent,
                        onClick = { activeSub = route },
                        modifier = Modifier.testTag("conflict_card_${route.name.lowercase()}")
                    )
                }
            }
        } else {
            WorldOrderHubHeader(
                title = activeSub!!.title,
                quote = "${home.name} • ${home.intelAgencyName} & High Command",
                onBack = { activeSub = null }
            )

            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (activeSub!!) {
                    ConflictSubRoute.ACTIVE_WARS -> {
                        item { PortfolioLockBanner(MinistryTag.DEFENSE, hasDefense, defHolder) }
                        item {
                            TargetCountrySelector(foreignTargets, targetIso, targetCountry) { targetIso = it }
                        }
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
                        if (state.conflicts.isEmpty()) {
                            item { OledCard { Text("No active frontlines right now.", style = MaterialTheme.typography.bodySmall, color = TextDescription) } }
                        } else {
                            items(state.conflicts, key = { it.id }) { war ->
                                OledCard(borderColor = AccentRed.copy(alpha = 0.5f)) {
                                    Text("${war.aggressorIso} vs ${war.defenderIso} • ${war.intensity}", style = MaterialTheme.typography.titleSmall, color = AccentRed)
                                    Text("Frontline: ${war.frontlineProvinceId} • Strength ${war.aggressorStrength} vs ${war.defenderStrength}", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                }
                            }
                        }
                    }

                    ConflictSubRoute.ARMED_FORCES -> {
                        item { PortfolioLockBanner(MinistryTag.DEFENSE, hasDefense, defHolder) }
                        item {
                            val a = state.militaryArsenal
                            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                                Text("${home.name} Armed Forces Readiness", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    ArsenalStat("Divisions", a.armyDivisions.toString())
                                    ArsenalStat("Squadrons", a.airSquadrons.toString())
                                    ArsenalStat("Fleets", a.navalFleets.toString())
                                    ArsenalStat("Readiness", "${a.readinessPct}%")
                                }
                            }
                        }
                        item {
                            val a = state.militaryArsenal
                            OledCard {
                                Text("Strategic Stockpiles", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Spacer(Modifier.height(6.dp))
                                Text("Rifles: ${a.rifles} • Ammunition: ${a.ammunition}", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                Text("Drones: ${a.drones} • Artillery: ${a.artillery}", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                Text("Main Battle Tanks: ${a.tanks} • Warships: ${a.warships}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                            }
                        }
                    }

                    ConflictSubRoute.MILITARY_PROCUREMENT -> {
                        item { PortfolioLockBanner(MinistryTag.DEFENSE, hasDefense, defHolder) }
                        val catalog = listOf(
                            Triple("Rifles", 500, 12000.0),
                            Triple("Ammunition", 10000, 9500.0),
                            Triple("Drones", 25, 22000.0),
                            Triple("Artillery", 15, 28000.0),
                            Triple("Tanks", 10, 45000.0),
                            Triple("Warships", 1, 95000.0)
                        )
                        items(catalog, key = { it.first }) { (kind, qty, cost) ->
                            OledCard {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("$kind Batch (+$qty units)", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Text("Contract Cost: ${formatUsd(cost)} • Requires [DEFENSE]", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                    }
                                    OledPrimaryButton("Order", { onBuyHardware(kind, qty, cost) }, enabled = hasDefense, accent = AccentGold)
                                }
                            }
                        }
                    }

                    ConflictSubRoute.INSURGENCIES -> {
                        item { PortfolioLockBanner(MinistryTag.SECURITY, hasSecurity, secHolder) }
                        item {
                            OledCard {
                                Text("Domestic Internal Security", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("National Stability: ${home.stability}% • Martial Order & Counter-Coup Sweeps", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                Spacer(Modifier.height(8.dp))
                                OledPrimaryButton(
                                    text = "Execute Counter-Intelligence Purge",
                                    enabled = hasSecurity,
                                    onClick = { onLaunchCovertOp(home.iso, CovertOpType.COUNTER_INTEL_PURGE) },
                                    modifier = Modifier.fillMaxWidth(),
                                    accent = AccentAmber
                                )
                            }
                        }
                    }

                    ConflictSubRoute.COVERT_INTEL -> {
                        item { PortfolioLockBanner(MinistryTag.INTELLIGENCE, hasIntel, intelHolder) }
                        item { TargetCountrySelector(foreignTargets, targetIso, targetCountry) { targetIso = it } }
                        item {
                            OledCard {
                                OledSectionHeader("Covert Intelligence & Sabotage", home.intelAgencyName, Icons.Filled.Security, AccentPurple)
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
                                                Text("Requires [${opType.requiredMinistry.code}]", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                                            }
                                            OledPrimaryButton("Execute", { onLaunchCovertOp(targetIso, opType) }, enabled = unlocked, accent = AccentPurple)
                                        }
                                    }
                                }
                            }
                        }
                        items(state.covertOps.take(10), key = { it.id }) { op ->
                            OledCard {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("${op.type.label} → ${op.targetCountryIso}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    OledBadge(op.status, if (op.status == "SUCCESS") AccentGreen else AccentRed)
                                }
                                Text("Success Odds: ${op.successChancePct}% • Day ${op.launchedDay}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                            }
                        }
                    }

                    ConflictSubRoute.PMCS -> {
                        item {
                            OledCard(borderColor = AccentGreen.copy(alpha = 0.5f)) {
                                Text("Private Military Contractors", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Text("Hire autonomous tactical battalions to safeguard foreign logistics or project private leverage.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                Spacer(Modifier.height(8.dp))
                                Text("Active PMC Battalions: ${state.militaryArsenal.pmcBattalions}", style = MaterialTheme.typography.titleSmall, color = AccentGreen)
                                Spacer(Modifier.height(8.dp))
                                OledPrimaryButton(
                                    text = "Contract PMC Battalion ($35,000)",
                                    onClick = onHirePmc,
                                    enabled = state.player.cashUsd >= 35_000.0,
                                    modifier = Modifier.fillMaxWidth(),
                                    accent = AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetCountrySelector(
    foreignTargets: List<Country>,
    targetIso: String,
    targetCountry: Country,
    onSelect: (String) -> Unit
) {
    OledCard {
        Text("Select Target Country", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            foreignTargets.forEach { c ->
                FilterChip(
                    selected = targetIso == c.iso,
                    onClick = { onSelect(c.iso) },
                    label = { Text("${c.iso} (${c.bilateralRelation})", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("${targetCountry.name} • Stability ${targetCountry.stability}% • Spy Network ${targetCountry.spyNetworkStrength}%", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
    }
}

@Composable
private fun ArsenalStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
    }
}

