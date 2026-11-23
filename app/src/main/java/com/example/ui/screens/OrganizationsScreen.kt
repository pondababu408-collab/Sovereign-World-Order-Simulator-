package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OrganizationsScreen(
    state: GameState,
    initialTab: Int = 0, // 0: Cabinet & Ministries, 1: Parliament, 2: 46 Laws, 3: 10 Constitution Chapters, 4: Parties, 5: Supranational Orgs
    onAppointMinister: (MinistryTag, String) -> Unit,
    onProposeLaw: (LawItem, String) -> Unit,
    onProposeConstitution: (ConstitutionalChapter) -> Unit,
    onCastVote: (String, String) -> Unit,
    onFoundParty: (String, String, String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Cabinet", "Chamber", "46 Laws", "Constitution", "Parties", "Supranational")
    val home = state.countries.first { it.iso == state.player.countryIso }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OledSectionHeader(
            title = "${home.name} • Governance & Orgs",
            subtitle = "${home.govType.label} • ${home.lowerChamberName}",
            icon = Icons.Filled.AccountBalance,
            accent = AccentGold
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) AccentGold.copy(alpha = 0.16f) else OledCard)
                        .border(1.dp, if (active) AccentGold else OledBorder, RoundedCornerShape(6.dp))
                        .clickable { selectedTab = idx }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (active) FontWeight.Bold else FontWeight.Normal),
                        color = if (active) AccentGold else TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> CabinetMinistriesTab(state, home, onAppointMinister)
                1 -> ParliamentChamberTab(state, home, onCastVote)
                2 -> LawsCatalogTab(state, onProposeLaw)
                3 -> ConstitutionChaptersTab(state, onProposeConstitution)
                4 -> PartiesTab(state, home, onFoundParty)
                5 -> SupranationalOrgsTab(state)
            }
        }
    }
}

@Composable
private fun CabinetMinistriesTab(
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
                    color = TextSecondary
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
                        Text(tag.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
private fun ParliamentChamberTab(
    state: GameState,
    home: Country,
    onCastVote: (String, String) -> Unit
) {
    val countryParties = state.parties.filter { it.countryIso == home.iso }
    val bills = state.activeBills.filter { it.countryIso == home.iso }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OledCard {
                OledSectionHeader("${home.lowerChamberName} & ${home.upperChamberName}", "Semi-Circular Seating Chart", Icons.Filled.Gavel, AccentCyan)
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

        item { Text("Active & Recent Parliamentary Bills", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(bills, key = { it.id }) { bill ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(bill.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                    OledBadge(bill.status, when (bill.status) { "PASSED" -> AccentGreen; "REJECTED" -> AccentRed; else -> AccentCyan })
                }
                Text("Proposed by ${bill.proposerName} • ${bill.hoursRemaining}h left", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
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
}

@Composable
private fun LawsCatalogTab(state: GameState, onProposeLaw: (LawItem, String) -> Unit) {
    var categoryFilter by remember { mutableStateOf("All") }
    val categories = remember(state.laws) { listOf("All") + state.laws.map { it.category }.distinct() }
    val filtered = remember(categoryFilter, state.laws) {
        if (categoryFilter == "All") state.laws else state.laws.filter { it.category == categoryFilter }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { cat ->
                FilterChip(selected = categoryFilter == cat, onClick = { categoryFilter = cat }, label = { Text(cat, style = MaterialTheme.typography.labelSmall) })
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(filtered, key = { it.id }) { law ->
                OledCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${law.code} • ${law.title}", style = MaterialTheme.typography.titleSmall, color = TextPrimary, modifier = Modifier.weight(1f))
                        OledBadge(law.requiredMinistry.code, AccentPurple)
                    }
                    Text(law.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

@Composable
private fun ConstitutionChaptersTab(state: GameState, onProposeConstitution: (ConstitutionalChapter) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                Text("10 Constitutional Chapters", style = MaterialTheme.typography.titleMedium, color = AccentGold)
                Text("Compare Current vs Proposed constitutional provisions and initiate parliamentary reform.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                        Text(ch.currentProvision, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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

@Composable
private fun PartiesTab(state: GameState, home: Country, onFoundParty: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var acronym by remember { mutableStateOf("") }
    var ideology by remember { mutableStateOf("Technocratic Pragmatism") }
    val localParties = state.parties.filter { it.countryIso == home.iso }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard {
                OledSectionHeader("Register Political Party ($15,000)", "Build coalition seats in ${home.lowerChamberName}", Icons.Filled.Groups, AccentCyan)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Party Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = acronym, onValueChange = { acronym = it }, label = { Text("Acronym") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = ideology, onValueChange = { ideology = it }, label = { Text("Ideology") }, singleLine = true, modifier = Modifier.weight(2f))
                }
                Spacer(Modifier.height(8.dp))
                OledPrimaryButton("Found Party ($15k)", { onFoundParty(name, acronym, ideology); name = ""; acronym = "" }, Modifier.fillMaxWidth(), enabled = name.isNotBlank() && acronym.isNotBlank())
            }
        }
        items(localParties, key = { it.id }) { p ->
            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${p.name} [${p.acronym}]", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${p.ideology} • ${p.seatsCount} seats", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OledBadge("${String.format("%.1f%%", p.approvalPct)}", AccentGold)
                }
            }
        }
    }
}

@Composable
private fun SupranationalOrgsTab(state: GameState) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.supranationalOrgs, key = { it.id }) { org ->
            val isMember = state.player.countryIso in org.memberIsos
            OledCard(borderColor = if (isMember) AccentCyan.copy(alpha = 0.5f) else OledBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Public, contentDescription = org.acronym, tint = AccentCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${org.name} (${org.acronym})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            if (isMember) OledBadge("MEMBER", AccentGreen)
                        }
                        Text(org.focus, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Active Resolution: ${org.activeResolution}", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                Text("Pool: $${String.format("%.1fB", org.treasuryBillions)} • Members: ${org.memberIsos.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}
