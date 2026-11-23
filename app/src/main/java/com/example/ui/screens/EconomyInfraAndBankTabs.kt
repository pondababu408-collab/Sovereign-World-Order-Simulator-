package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun ElectricalGridTab(
    state: GameState,
    onBuildSubstationOrUpgrade: (String) -> Unit,
    onBuildTransmission: (String, String, String) -> Unit
) {
    val playerCompanies = state.companies.filter { it.ownerId == state.player.id }
    val currentProv = state.provinces.firstOrNull { it.id == state.player.currentRegionId }
    val regionPlants = state.powerPlants.filter { it.regionId == state.player.currentRegionId }
    val regionSubs = state.substations.filter { it.regionId == state.player.currentRegionId }
    val homeProvinces = state.provinces.filter { it.countryIso == state.player.countryIso }
    var targetDestReg by remember(homeProvinces) { mutableStateOf(homeProvinces.lastOrNull()?.id ?: "") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                OledSectionHeader(
                    "3-Tier Regional Electrical Grid",
                    "Tier 1: Power Plants → Tier 2: Transmission → Tier 3: Substations",
                    Icons.Filled.Bolt,
                    AccentGold
                )
                Spacer(Modifier.height(6.dp))
                if (currentProv != null) {
                    Text("${currentProv.name} Grid Load", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    val ratio = (currentProv.powerSupplyMw.toFloat() / currentProv.powerDemandMw.coerceAtLeast(1)).coerceIn(0f, 1.5f)
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (ratio >= 1f) AccentGreen else AccentRed,
                        trackColor = OledElevated
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Supply: ${currentProv.powerSupplyMw} MW • Demand: ${currentProv.powerDemandMw} MW", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                }
            }
        }

        // Player Grid Controls
        item {
            OledCard {
                Text("Operate Regional Substation & Transmission", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Max 1 Substation per company per region; upgradeable capacity.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                val comp = playerCompanies.firstOrNull()
                if (comp == null) {
                    Text("Found a company (e.g. Power Plant) in My Businesses to build Substations & Transmission Lines.", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OledPrimaryButton(
                            text = "Build/Upgrade Substation",
                            onClick = { onBuildSubstationOrUpgrade(comp.id) },
                            modifier = Modifier.weight(1f),
                            accent = AccentGold
                        )
                        if (homeProvinces.size > 1) {
                            OledOutlinedButton(
                                text = "Link Grid ($12k)",
                                onClick = { onBuildTransmission(state.player.currentRegionId, targetDestReg, comp.id) },
                                modifier = Modifier.weight(1f),
                                accent = AccentCyan
                            )
                        }
                    }
                }
            }
        }

        item {
            OledSectionHeader("Tier 1 • Power Plants (${state.powerPlants.size})", "Nuclear, Coal, Gas, Solar, Wind")
        }
        items(regionPlants.ifEmpty { state.powerPlants.take(8) }, key = { it.id }) { pp ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(pp.companyName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${pp.fuel.displayName} • ${pp.regionId}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OledBadge("${pp.activeOutputMw}/${pp.capacityMw} MW", AccentGreen)
                }
            }
        }

        item {
            OledSectionHeader("Tier 2 • Transmission Grids (${state.transmissionLines.size})", "Inter-regional high-voltage links")
        }
        items(state.transmissionLines.take(10), key = { it.id }) { tx ->
            OledCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("${tx.fromRegionId} ⇄ ${tx.toRegionId}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Text("${tx.maxTransferMw} MW Cap", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                }
            }
        }

        item {
            OledSectionHeader("Tier 3 • Substations (${regionSubs.size} local)", "Last-mile distribution")
        }
        items(regionSubs.ifEmpty { state.substations.take(8) }, key = { it.id }) { sub ->
            OledCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("${sub.companyName} (${sub.regionId})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    OledBadge("Lv.${sub.level} • ${sub.capacityMw} MW", AccentGold)
                }
            }
        }
    }
}

@Composable
fun CentralBankTab(
    state: GameState,
    onUpdateCentralBank: (Double, Double, Double, Boolean) -> Unit
) {
    val home = state.countries.first { it.iso == state.player.countryIso }
    val hasAccess = SimulationEngine.hasPortfolioAccess(state, MinistryTag.CENTRAL_BANK)
    val holderId = home.ministerAssignments[MinistryTag.CENTRAL_BANK.name]
    val holderName = if (holderId == state.player.id) state.player.name else state.npcs.firstOrNull { it.id == holderId }?.name ?: "Vacant"

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PortfolioLockBanner(MinistryTag.CENTRAL_BANK, hasAccess, holderName)
        }
        item {
            OledCard {
                OledSectionHeader(
                    home.centralBankName,
                    "Sovereign Monetary Authority & SWIFT Gateway",
                    Icons.Filled.AccountBalance,
                    AccentCyan
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricBlock("M0 Base", "$${String.format("%.1fB", home.m0BillionsUsd)}")
                    MetricBlock("M1 Narrow", "$${String.format("%.1fB", home.m1BillionsUsd)}")
                    MetricBlock("Policy Rate", "${String.format("%.2f%%", home.interestRatePct)}")
                    MetricBlock("Reserve Ratio", "${String.format("%.1f%%", home.reserveRatioPct)}")
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Inflation: ${String.format("%.2f%%", home.inflationPct)}", style = MaterialTheme.typography.labelMedium, color = if (home.inflationPct > 6) AccentRed else AccentGreen)
                    Text("FX: ${String.format("%.2f", home.forexPerUsd)} ${home.currencyCode}/USD", style = MaterialTheme.typography.labelMedium, color = AccentGold)
                    OledBadge(if (home.swiftConnected) "SWIFT ONLINE" else "SWIFT DISCONNECTED", if (home.swiftConnected) AccentGreen else AccentRed)
                }
            }
        }

        item {
            OledCard {
                Text("Monetary Policy & Liquidity Controls", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OledOutlinedButton("Rate +0.50%", { onUpdateCentralBank(0.5, 0.0, 0.0, false) }, Modifier.weight(1f), hasAccess, AccentRed)
                    OledOutlinedButton("Rate -0.50%", { onUpdateCentralBank(-0.5, 0.0, 0.0, false) }, Modifier.weight(1f), hasAccess, AccentGreen)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OledOutlinedButton("QE +$5B M0", { onUpdateCentralBank(0.0, 0.0, 5.0, false) }, Modifier.weight(1f), hasAccess, AccentCyan)
                    OledOutlinedButton("QT -$5B M0", { onUpdateCentralBank(0.0, 0.0, -5.0, false) }, Modifier.weight(1f), hasAccess, AccentGold)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OledOutlinedButton("Reserve +2%", { onUpdateCentralBank(0.0, 2.0, 0.0, false) }, Modifier.weight(1f), hasAccess, AccentPurple)
                    OledOutlinedButton("Toggle SWIFT", { onUpdateCentralBank(0.0, 0.0, 0.0, true) }, Modifier.weight(1f), hasAccess, AccentRed)
                }
            }
        }
    }
}

@Composable
fun CentralPlanningTab(
    state: GameState,
    onUpdatePlanning: (Int, Double, Double) -> Unit
) {
    val home = state.countries.first { it.iso == state.player.countryIso }
    val isPlanned = home.economicSystem == EconomicSystem.PLANNED
    val hasAccess = SimulationEngine.hasPortfolioAccess(state, MinistryTag.CENTRAL_PLANNING)
    val holderId = home.ministerAssignments[MinistryTag.CENTRAL_PLANNING.name]
    val holderName = if (holderId == state.player.id) state.player.name else state.npcs.firstOrNull { it.id == holderId }?.name ?: "Vacant"

    var rationKg by remember(home.rationQuotaKgPerPop) { mutableIntStateOf(home.rationQuotaKgPerPop.coerceAtLeast(10)) }
    var wageMult by remember(home.wageFloorMultiplier) { mutableDoubleStateOf(home.wageFloorMultiplier) }
    var targetGdp by remember(home.fiveYearQuotaTargetGdp) { mutableDoubleStateOf(home.fiveYearQuotaTargetGdp) }

    val soes = state.companies.filter { it.countryIso == home.iso && (it.isStateOwnedEnterprise || it.stateShares > 5000L) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            PortfolioLockBanner(MinistryTag.CENTRAL_PLANNING, hasAccess && isPlanned, holderName)
        }
        if (!isPlanned) {
            item {
                OledCard(borderColor = AccentGold.copy(alpha = 0.6f)) {
                    Text("Comité Planificador Central Locked", style = MaterialTheme.typography.titleMedium, color = AccentGold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${home.name} currently operates under ${home.economicSystem.label}. Pass Law ECO-01 (Economic System Charter -> Planned Economy) in Laws & Norms or start in Cuba to unlock Central Planning.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        item {
            OledCard {
                OledSectionHeader(
                    "Comité Planificador Central",
                    "Pop Ration Quotas (Cupos), Wage Fixing & 5-Year Plans",
                    Icons.Filled.PrecisionManufacturing,
                    AccentRed
                )
                Spacer(Modifier.height(8.dp))
                Text("Free Pop Ration Quota (Cupos): $rationKg kg/citizen", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Slider(
                    value = rationKg.toFloat(),
                    onValueChange = { rationKg = it.roundToInt() },
                    valueRange = 5f..80f,
                    enabled = isPlanned && hasAccess
                )
                Text("Regional Wage Floor Multiplier: ${String.format("%.2fx", wageMult)}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Slider(
                    value = wageMult.toFloat(),
                    onValueChange = { wageMult = it.toDouble() },
                    valueRange = 0.6f..2.5f,
                    enabled = isPlanned && hasAccess
                )
                Text("5-Year GDP Quota Target: $${String.format("%.1fB", targetGdp)}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                Spacer(Modifier.height(6.dp))
                OledPrimaryButton(
                    text = "Enact Central Plan Directive",
                    onClick = { onUpdatePlanning(rationKg, wageMult, targetGdp * 1.05) },
                    enabled = isPlanned && hasAccess,
                    modifier = Modifier.fillMaxWidth(),
                    accent = AccentGold
                )
            }
        }

        item {
            OledSectionHeader("Sovereign Wealth >50% Controlled SOEs (${soes.size})", "Board & price fixing roster")
        }
        items(soes, key = { it.id }) { comp ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CorporateLogo(comp.ticker)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(comp.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${comp.type.displayName} • State Shares: ${(comp.stateShares * 100 / comp.totalShares)}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OledBadge(formatUsd(comp.valuationUsd), AccentGold)
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = AccentCyan)
    }
}
