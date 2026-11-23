package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

private enum class OrgSubView {
    HUB,
    FOUND_ORGANIZATION,
    LINKED_OUT_JOBS,
    RANKING
}

private data class OrgRoleEntry(
    val orgName: String,
    val handle: String,
    val roleBadge: String,
    val category: String,
    val seed: Int
)

@Composable
fun OrganizationsScreen(
    state: GameState,
    onApplyJob: (String) -> Unit,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onFoundParty: (String, String, String) -> Unit
) {
    var subView by remember { mutableStateOf(OrgSubView.HUB) }
    val home = state.countries.first { it.iso == state.player.countryIso }

    BackHandler(enabled = subView != OrgSubView.HUB) {
        subView = OrgSubView.HUB
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        when (subView) {
            OrgSubView.HUB -> {
                WorldOrderHubHeader(
                    title = "Organizations",
                    quote = "\"I would never belong to a club that would admit someone like me as a member\" - Groucho Marx"
                )

                val myRoles = remember(state.player, state.parties, state.companies, home) {
                    buildList {
                        state.player.officesHeld.forEach { tag ->
                            val roleTitle = when (tag) {
                                MinistryTag.HEAD_OF_STATE -> "PRESIDENT"
                                MinistryTag.CENTRAL_BANK -> "GOVERNOR"
                                MinistryTag.CENTRAL_PLANNING -> "CHAIRPERSON"
                                else -> "MINISTER"
                            }
                            add(
                                OrgRoleEntry(
                                    orgName = tag.title,
                                    handle = "@${home.iso.lowercase()}_${tag.code.lowercase()}",
                                    roleBadge = roleTitle,
                                    category = "Executive Office",
                                    seed = tag.ordinal * 17
                                )
                            )
                        }
                        if (state.player.isParliamentMember) {
                            add(
                                OrgRoleEntry(
                                    orgName = home.lowerChamberName,
                                    handle = "@${home.iso.lowercase()}_chamber",
                                    roleBadge = "MP / DEPUTY",
                                    category = "Legislature",
                                    seed = 91
                                )
                            )
                        }
                        state.parties.firstOrNull { it.id == state.player.partyId }?.let { party ->
                            add(
                                OrgRoleEntry(
                                    orgName = party.name,
                                    handle = "@${party.acronym.lowercase()}_${party.countryIso.lowercase()}",
                                    roleBadge = if (party.leaderId == state.player.id) "CHAIRPERSON" else "MEMBER",
                                    category = "Political Party",
                                    seed = party.name.hashCode()
                                )
                            )
                        }
                        state.companies.filter { it.ownerId == state.player.id }.forEach { comp ->
                            add(
                                OrgRoleEntry(
                                    orgName = comp.name,
                                    handle = "@${comp.ticker.lowercase()}",
                                    roleBadge = "CHIEF OF STAFF",
                                    category = comp.type.sector,
                                    seed = comp.name.hashCode()
                                )
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("My Organizations", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }

                    if (myRoles.isEmpty()) {
                        item {
                            OledCard {
                                Text("No active executive offices or company chairs yet.", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                                Text("Apply for a career on LinkedOut or found a corporation or political party below.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    } else {
                        val pairs = myRoles.chunked(2)
                        items(pairs) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowItems.forEach { entry ->
                                    OledCard(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            ExecutivePortrait(seed = entry.seed, name = entry.orgName, size = 34.dp, accent = AccentGold)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(entry.orgName, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(entry.handle, style = MaterialTheme.typography.labelSmall, color = TextDescription, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        OledBadge(entry.roleBadge, AccentGold)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("My Associations & Membership Applications", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                    }

                    val activeJob = state.jobListings.firstOrNull { it.id == state.player.currentJobId }
                    val pendingInvites = state.officialMails.filter { it.status == "PENDING" && it.type in setOf(MailType.PARTY_COFOUND_INVITE, MailType.PARLIAMENT_INVITE, MailType.MINISTRY_OFFER) }
                    item {
                        OledCard {
                            if (activeJob != null) {
                                Text("Employment: ${activeJob.roleTitle} at ${activeJob.companyName}", style = MaterialTheme.typography.titleSmall, color = AccentGreen)
                            } else {
                                Text("Associations: ${home.allianceBloc} Citizen Registry", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (pendingInvites.isEmpty()) "No pending membership applications. Check Official Mail for coalition or cabinet invites." else "${pendingInvites.size} pending invitations awaiting your review in Messages.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDescription
                            )
                        }
                    }

                    // Three White Pill CTA Buttons matching reference
                    item {
                        Spacer(Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            WhitePillButton(
                                text = "Found organization",
                                onClick = { subView = OrgSubView.FOUND_ORGANIZATION },
                                modifier = Modifier.fillMaxWidth().testTag("orgs_found_btn")
                            )
                            WhitePillButton(
                                text = "Search for jobs",
                                onClick = { subView = OrgSubView.LINKED_OUT_JOBS },
                                modifier = Modifier.fillMaxWidth().testTag("orgs_jobs_btn")
                            )
                            WhitePillButton(
                                text = "Ranking",
                                onClick = { subView = OrgSubView.RANKING },
                                modifier = Modifier.fillMaxWidth().testTag("orgs_ranking_btn")
                            )
                        }
                    }
                }
            }

            OrgSubView.FOUND_ORGANIZATION -> {
                WorldOrderHubHeader(
                    title = "Found organization",
                    quote = "Establish a Political Party or Sovereign Enterprise in ${home.name}",
                    onBack = { subView = OrgSubView.HUB }
                )
                FoundOrganizationSubScreen(
                    state = state,
                    home = home,
                    onFoundCompany = onFoundCompany,
                    onFoundParty = onFoundParty
                )
            }

            OrgSubView.LINKED_OUT_JOBS -> {
                WorldOrderHubHeader(
                    title = "LinkedOut Careers",
                    quote = "Daily salary scales with Efficiency (${state.player.efficiency}) & Intelligence (${state.player.intelligence})",
                    onBack = { subView = OrgSubView.HUB }
                )
                LinkedOutSubScreen(state = state, onApplyJob = onApplyJob)
            }

            OrgSubView.RANKING -> {
                WorldOrderHubHeader(
                    title = "Organization Ranking",
                    quote = "Top Corporations & Political Movements Worldwide",
                    onBack = { subView = OrgSubView.HUB }
                )
                OrganizationRankingSubScreen(state = state)
            }
        }
    }
}

@Composable
private fun FoundOrganizationSubScreen(
    state: GameState,
    home: Country,
    onFoundCompany: (String, BusinessType, PowerFuel?) -> Unit,
    onFoundParty: (String, String, String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0: Company, 1: Political Party
    var name by remember { mutableStateOf("") }
    var acronym by remember { mutableStateOf("") }
    var ideology by remember { mutableStateOf("Technocratic Pragmatism") }
    var selectedType by remember { mutableStateOf(BusinessType.FARM) }
    var selectedFuel by remember { mutableStateOf(PowerFuel.SOLAR) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Enterprise / Corp") })
                FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Political Party ($15k)") })
            }
        }
        item {
            if (mode == 0) {
                OledCard {
                    Text("Incorporate Business", style = MaterialTheme.typography.titleSmall, color = AccentGold)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Corporation Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BusinessType.entries.forEach { t ->
                            FilterChip(selected = selectedType == t, onClick = { selectedType = t }, label = { Text("${t.displayName} (${formatUsd(t.baseCostUsd)})", style = MaterialTheme.typography.labelSmall) })
                        }
                    }
                    if (selectedType == BusinessType.POWER_PLANT) {
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PowerFuel.entries.forEach { f ->
                                FilterChip(selected = selectedFuel == f, onClick = { selectedFuel = f }, label = { Text(f.displayName, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OledPrimaryButton(
                        text = "Incorporate (${formatUsd(selectedType.baseCostUsd)})",
                        onClick = { onFoundCompany(name, selectedType, selectedFuel); name = "" },
                        enabled = name.isNotBlank() && state.player.cashUsd >= selectedType.baseCostUsd,
                        modifier = Modifier.fillMaxWidth(),
                        accent = AccentGold
                    )
                }
            } else {
                OledCard {
                    Text("Register Political Party in ${home.lowerChamberName}", style = MaterialTheme.typography.titleSmall, color = AccentCyan)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Party Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = acronym, onValueChange = { acronym = it }, label = { Text("Acronym") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = ideology, onValueChange = { ideology = it }, label = { Text("Ideology") }, singleLine = true, modifier = Modifier.weight(2f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OledPrimaryButton(
                        text = "Found Party ($15,000)",
                        onClick = { onFoundParty(name, acronym, ideology); name = ""; acronym = "" },
                        enabled = name.isNotBlank() && acronym.isNotBlank() && state.player.cashUsd >= 15_000.0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LinkedOutSubScreen(state: GameState, onApplyJob: (String) -> Unit) {
    val homeJobs = state.jobListings.filter { it.countryIso == state.player.countryIso }
    val globalJobs = state.jobListings.filter { it.countryIso != state.player.countryIso }.take(20)
    val workerScore = (state.player.efficiency + state.player.intelligence) / 2.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                val activeJob = state.jobListings.firstOrNull { it.id == state.player.currentJobId }
                if (activeJob != null) {
                    val estPay = activeJob.salaryPerPointUsd * workerScore
                    Text("Active Role: ${activeJob.roleTitle} at ${activeJob.companyName}", style = MaterialTheme.typography.titleSmall, color = AccentGreen)
                    Text("Daily Pay: ${formatUsd(estPay)}/day", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                } else {
                    Text("Unemployed • Apply to any position below to start earning daily salary.", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                }
            }
        }
        item { Text("Domestic Positions (${state.player.countryIso})", style = MaterialTheme.typography.titleSmall, color = TextPrimary) }
        items(homeJobs, key = { it.id }) { job ->
            OrgJobRow(job, workerScore, state.player.currentJobId == job.id, onApplyJob)
        }
        item { Text("International Positions", style = MaterialTheme.typography.titleSmall, color = TextSecondary) }
        items(globalJobs, key = { it.id }) { job ->
            OrgJobRow(job, workerScore, state.player.currentJobId == job.id, onApplyJob)
        }
    }
}

@Composable
private fun OrgJobRow(job: JobListing, workerScore: Double, isCurrent: Boolean, onApply: (String) -> Unit) {
    val dailyPay = job.salaryPerPointUsd * workerScore
    OledCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(job.roleTitle, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    if (job.isStateJob) OledBadge("SOE", AccentGold)
                }
                Text("${job.companyName} (${job.countryIso})", style = MaterialTheme.typography.bodySmall, color = TextDescription)
                Text("Pay: $${String.format("%.2f", job.salaryPerPointUsd)}/pt (~${formatUsd(dailyPay)}/day) • Min INT ${job.minIntelligence}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
            }
            OledPrimaryButton(
                text = if (isCurrent) "Active" else "Apply",
                enabled = !isCurrent,
                onClick = { onApply(job.id) },
                accent = if (isCurrent) AccentGreen else AccentCyan
            )
        }
    }
}

@Composable
private fun OrganizationRankingSubScreen(state: GameState) {
    val topCompanies = remember(state.companies) { state.companies.sortedByDescending { it.valuationUsd }.take(25) }
    val topParties = remember(state.parties) { state.parties.sortedByDescending { it.seatsCount }.take(15) }
    var tab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Corporations") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Political Parties") })
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) {
                itemsIndexed(topCompanies, key = { _, c -> c.id }) { idx, c ->
                    OledCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelMedium, color = AccentGold, modifier = Modifier.width(30.dp))
                            CorporateLogo(c.ticker, 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${c.countryIso} • ${c.type.displayName}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                            }
                            Text(formatUsd(c.valuationUsd), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AccentGreen)
                        }
                    }
                }
            } else {
                itemsIndexed(topParties, key = { _, p -> p.id }) { idx, p ->
                    OledCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("#${idx + 1}", style = MaterialTheme.typography.labelMedium, color = AccentGold, modifier = Modifier.width(30.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${p.name} [${p.acronym}]", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${p.countryIso} • ${p.ideology}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                            }
                            OledBadge("${p.seatsCount} seats", AccentCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SupranationalOrgsScreen(state: GameState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        WorldOrderHubHeader(
            title = "Supranational organizations",
            quote = "\"Power is not a means; it is an end.\" - Supranational Blocs & Multilateral Councils"
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
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
                            Text(org.focus, style = MaterialTheme.typography.bodySmall, color = TextDescription)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Active Resolution: ${org.activeResolution}", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                    Text("Pool: $${String.format("%.1fB", org.treasuryBillions)} • Members: ${org.memberIsos.joinToString(", ")}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
        }
    }
}

