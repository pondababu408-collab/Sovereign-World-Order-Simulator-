package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

enum class RulesAdminSubRoute(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: Color
) {
    CONSTITUTION("Constitution", "10 Constitutional Chapters comparing Current vs Proposed charters.", Icons.Filled.MenuBook, AccentGold),
    ELECTORAL_CHAMBER("Electoral Chamber", "Hemicycle legislature, 46 active laws & live NPC roll-call votes.", Icons.Filled.Gavel, AccentCyan),
    CABINET_MINISTRIES("Cabinet & Ministries", "Strict Portfolio-Tag isolation across 13 executive departments.", Icons.Filled.AdminPanelSettings, AccentPurple),
    CENTRAL_PLANNING("Central Planning", "Planned Economy SOEs, Pop Ration Quotas (Cupos) & wage floors.", Icons.Filled.PrecisionManufacturing, AccentRed),
    ELECTRICAL_GRID("Electrical Grid", "3-tier power plants, inter-regional transmission & substations.", Icons.Filled.Bolt, AccentGold),
    STATE_AGENCIES("State Agencies", "RBI, Banque de France, Fed, PBOC, R&AW, DGSE, CIA & space bureaus.", Icons.Filled.AccountBalance, AccentGreen)
}

@Composable
fun RulesAndAdminScreen(
    state: GameState,
    initialSubRoute: RulesAdminSubRoute? = null,
    onAppointMinister: (MinistryTag, String) -> Unit,
    onProposeLaw: (LawItem, String) -> Unit,
    onProposeConstitution: (ConstitutionalChapter) -> Unit,
    onCastVote: (String, String) -> Unit,
    onUpdatePlanning: (Int, Double, Double) -> Unit,
    onBuildSubstationOrUpgrade: (String) -> Unit,
    onBuildTransmission: (String, String, String) -> Unit
) {
    var activeSub by remember(initialSubRoute) { mutableStateOf(initialSubRoute) }
    val home = state.countries.first { it.iso == state.player.countryIso }

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
                title = "Rules & Administration",
                quote = "\"Law is order, and good law is good order.\" - Aristotle"
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(RulesAdminSubRoute.entries, key = { it.name }) { route ->
                    HubMenuCard(
                        title = route.title,
                        description = route.description,
                        icon = route.icon,
                        accent = route.accent,
                        onClick = { activeSub = route },
                        modifier = Modifier.testTag("rules_card_${route.name.lowercase()}")
                    )
                }
            }
        } else {
            WorldOrderHubHeader(
                title = activeSub!!.title,
                quote = "${home.name} • ${home.govType.label} • ${home.lowerChamberName}",
                onBack = { activeSub = null }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (activeSub!!) {
                    RulesAdminSubRoute.CONSTITUTION -> ConstitutionChaptersPanel(state, onProposeConstitution)
                    RulesAdminSubRoute.ELECTORAL_CHAMBER -> ElectoralChamberAndLawsPanel(state, home, onCastVote, onProposeLaw)
                    RulesAdminSubRoute.CABINET_MINISTRIES -> CabinetMinistriesPanel(state, home, onAppointMinister)
                    RulesAdminSubRoute.CENTRAL_PLANNING -> CentralPlanningTab(state, onUpdatePlanning)
                    RulesAdminSubRoute.ELECTRICAL_GRID -> ElectricalGridTab(state, onBuildSubstationOrUpgrade, onBuildTransmission)
                    RulesAdminSubRoute.STATE_AGENCIES -> StateAgenciesPanel(state, home)
                }
            }
        }
    }
}

@Composable
private fun CabinetMinistriesPanel(
    state: GameState,
    home: Country,
    onAppointMinister: (MinistryTag, String) -> Unit
) {
    val isHos = SimulationEngine.hasPortfolioAccess(state, MinistryTag.HEAD_OF_STATE)
    var appointingTag by remember { mutableStateOf<MinistryTag?>(null) }
    val localNpcs = state.npcs.filter { it.countryIso == home.iso }.take(14)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard(borderColor = AccentPurple.copy(alpha = 0.5f)) {
                Text("Strict Portfolio-Tag Isolation", style = MaterialTheme.typography.titleSmall, color = AccentPurple)
                Text(
                    "Each Ministry is a separate organization with exclusive Jurisdiction Tags. Only the appointed officeholder can operate its controls.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDescription
                )
            }
        }

        items(MinistryTag.entries, key = { it.name }) { tag ->
            val holderId = home.ministerAssignments[tag.name]
            val isPlayer = holderId == state.player.id || tag in state.player.officesHeld
            val holderName = if (isPlayer) "${state.player.name} (YOU)" else state.npcs.firstOrNull { it.id == holderId }?.name ?: "Vacant"

            OledCard(borderColor = if (isPlayer) AccentGreen.copy(alpha = 0.6f) else OledBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tag.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            OledBadge("[${tag.code}]", if (isPlayer) AccentGreen else AccentCyan)
                        }
                        Text(tag.description, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        Spacer(Modifier.height(2.dp))
                        Text("Officeholder: $holderName", style = MaterialTheme.typography.labelSmall, color = if (isPlayer) AccentGreen else AccentGold)
                    }
                    if (isHos && tag != MinistryTag.HEAD_OF_STATE) {
                        OledOutlinedButton("Assign", { appointingTag = tag }, accent = AccentGold)
                    }
                }
            }
        }
    }

    if (appointingTag != null) {
        val tag = appointingTag!!
        AlertDialog(
            onDismissRequest = { appointingTag = null },
            containerColor = OledCard,
            title = { Text("Appoint ${tag.title}", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(AccentGreen.copy(alpha = 0.14f))
                                .clickable {
                                    onAppointMinister(tag, state.player.id)
                                    appointingTag = null
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${state.player.name} (Assume Portfolio Yourself)", style = MaterialTheme.typography.titleSmall, color = AccentGreen)
                            OledBadge("YOU", AccentGreen)
                        }
                    }
                    items(localNpcs, key = { it.id }) { npc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OledElevated)
                                .clickable {
                                    onAppointMinister(tag, npc.id)
                                    appointingTag = null
                                }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${npc.name} (Lv.${npc.level} • ${npc.ambition.label})", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            OledBadge(npc.traitTier.name, AccentCyan)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { appointingTag = null }) { Text("Cancel", color = TextSecondary) } }
        )
    }
}

@Composable
private fun ElectoralChamberAndLawsPanel(
    state: GameState,
    home: Country,
    onCastVote: (String, String) -> Unit,
    onProposeLaw: (LawItem, String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0: Chamber & Bills, 1: 46 Laws Catalog
    var categoryFilter by remember { mutableStateOf("All") }
    val categories = remember(state.laws) { listOf("All") + state.laws.map { it.category }.distinct() }
    val filteredLaws = remember(categoryFilter, state.laws) {
        if (categoryFilter == "All") state.laws else state.laws.filter { it.category == categoryFilter }
    }
    val countryParties = state.parties.filter { it.countryIso == home.iso }
    val bills = state.activeBills.filter { it.countryIso == home.iso }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Hemicycle & Bills") })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("46 Statutes (${state.laws.size})") })
        }
        Spacer(Modifier.height(8.dp))

        if (mode == 0) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OledCard {
                        Text("${home.lowerChamberName} & ${home.upperChamberName}", style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                        Spacer(Modifier.height(8.dp))
                        ParliamentHemicycleCanvas(parties = countryParties)
                        Spacer(Modifier.height(8.dp))
                        countryParties.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${p.name} [${p.acronym}]", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                Text("${p.seatsCount} seats (${String.format("%.1f%%", p.approvalPct)})", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                            }
                        }
                    }
                }
                item { Text("Active & Recent Bills (${bills.size})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
                items(bills, key = { it.id }) { bill ->
                    OledCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(bill.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                            OledBadge(bill.status, when (bill.status) { "PASSED" -> AccentGreen; "REJECTED" -> AccentRed; else -> AccentCyan })
                        }
                        Text("Proposed by ${bill.proposerName} • ${bill.hoursRemaining}h left", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Current: ${bill.currentText}", style = MaterialTheme.typography.bodySmall, color = TextMuted, modifier = Modifier.weight(1f))
                            Text("Proposed: ${bill.proposedText}", style = MaterialTheme.typography.bodySmall, color = AccentCyan, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Aye: ${bill.votesFor} • Nay: ${bill.votesAgainst} • Abstain: ${bill.votesAbstain}", style = MaterialTheme.typography.labelMedium, color = AccentGold)
                        if (bill.status == "ACTIVE" && bill.playerVoted == null) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OledPrimaryButton("Vote Aye", { onCastVote(bill.id, "FOR") }, Modifier.weight(1f), accent = AccentGreen)
                                OledOutlinedButton("Vote Nay", { onCastVote(bill.id, "AGAINST") }, Modifier.weight(1f), accent = AccentRed)
                            }
                        }
                    }
                }
            }
        } else {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
                }
            }
            Spacer(Modifier.height(6.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filteredLaws, key = { it.id }) { law ->
                    OledCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("${law.code} • ${law.title}", style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                            OledBadge(law.requiredMinistry.code, AccentPurple)
                        }
                        Text(law.description, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        Spacer(Modifier.height(4.dp))
                        Text("Active Statute: ${law.currentValue}", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            law.proposedOptions.filter { it != law.currentValue }.forEach { opt ->
                                OledOutlinedButton("Propose: $opt", { onProposeLaw(law, opt) }, accent = AccentCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConstitutionChaptersPanel(state: GameState, onProposeConstitution: (ConstitutionalChapter) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Text("10 Constitutional Chapters", style = MaterialTheme.typography.titleMedium, color = AccentGold)
                Text("Compare Current vs Proposed constitutional provisions and initiate parliamentary reform.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
            }
        }
        items(state.constitutionalChapters, key = { it.chapterNumber }) { ch ->
            OledCard {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Chapter ${ch.chapterNumber}: ${ch.title}", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (ch.isReformed) OledBadge("REFORMED", AccentGreen)
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(OledElevated).padding(8.dp)) {
                        Text("CURRENT", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(ch.currentProvision, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                    }
                    Column(modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(OledElevated).padding(8.dp)) {
                        Text("PROPOSED", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        Text(ch.proposedProvision, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
                if (!ch.isReformed) {
                    Spacer(Modifier.height(8.dp))
                    OledPrimaryButton("Table Constitutional Reform", { onProposeConstitution(ch) }, Modifier.fillMaxWidth(), accent = AccentGold)
                }
            }
        }
    }
}

private data class StateAgencyItem(
    val name: String,
    val role: String,
    val requiredTag: MinistryTag
)

@Composable
private fun StateAgenciesPanel(state: GameState, home: Country) {
    val agencies = remember(home.iso) {
        val base = when (home.iso) {
            "IN" -> listOf(
                StateAgencyItem("Reserve Bank of India (RBI)", "Monetary Stability & Sovereign Forex Reserves", MinistryTag.CENTRAL_BANK),
                StateAgencyItem("Research & Analysis Wing (R&AW)", "External Intelligence & Covert Reconnaissance", MinistryTag.INTELLIGENCE),
                StateAgencyItem("ISRO Strategic Directorate", "Satellite Surveillance & Orbital Telemetry", MinistryTag.ENERGY),
                StateAgencyItem("NITI Planning Commission", "National Industrial & Infrastructure Corridors", MinistryTag.ECONOMY)
            )
            "FR" -> listOf(
                StateAgencyItem("Banque de France", "Eurozone Reserve & Liquidity Operations", MinistryTag.CENTRAL_BANK),
                StateAgencyItem("DGSE Directorate", "Foreign Intelligence & Cyber Defense", MinistryTag.INTELLIGENCE),
                StateAgencyItem("EDF Nuclear Authority", "Strategic Atomic Reactor Output", MinistryTag.ENERGY)
            )
            "US" -> listOf(
                StateAgencyItem("Federal Reserve System", "Dollar Liquidity, Rates & SWIFT Clearing", MinistryTag.CENTRAL_BANK),
                StateAgencyItem("Central Intelligence Agency (CIA)", "Global Intelligence & Clandestine Networks", MinistryTag.INTELLIGENCE),
                StateAgencyItem("Department of Defense (Pentagon)", "Global Power Projection & Carrier Fleets", MinistryTag.DEFENSE)
            )
            "CN" -> listOf(
                StateAgencyItem("People's Bank of China (PBOC)", "Renminbi Peg & Capital Controls", MinistryTag.CENTRAL_BANK),
                StateAgencyItem("NDRC Planning Bureau", "State-Owned Enterprise 5-Year Quotas", MinistryTag.CENTRAL_PLANNING),
                StateAgencyItem("Ministry of State Security", "Counter-Intelligence & Tech Shield", MinistryTag.INTELLIGENCE)
            )
            else -> listOf(
                StateAgencyItem(home.centralBankName, "Sovereign Monetary & Currency Directorate", MinistryTag.CENTRAL_BANK),
                StateAgencyItem(home.intelAgencyName, "National Intelligence & Reconnaissance Bureau", MinistryTag.INTELLIGENCE),
                StateAgencyItem("${home.name} Treasury Bureau", "Fiscal Revenue & Sovereign Debt Operations", MinistryTag.TREASURY),
                StateAgencyItem("${home.name} Grid Regulator", "Energy Permits & Power Allocation", MinistryTag.ENERGY)
            )
        }
        base
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                Text("${home.name} State Agencies", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Text("Statutory bureaus gated strictly by your held Ministry & Directorate jurisdiction tags.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
            }
        }
        items(agencies, key = { it.name }) { ag ->
            val unlocked = SimulationEngine.hasPortfolioAccess(state, ag.requiredTag)
            OledCard(borderColor = if (unlocked) AccentGreen.copy(alpha = 0.5f) else OledBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ag.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(ag.role, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        Text("Requires [${ag.requiredTag.code}]", style = MaterialTheme.typography.labelSmall, color = if (unlocked) AccentGreen else AccentRed)
                    }
                    OledBadge(if (unlocked) "UNLOCKED" else "LOCKED", if (unlocked) AccentGreen else AccentRed)
                }
            }
        }
    }
}
