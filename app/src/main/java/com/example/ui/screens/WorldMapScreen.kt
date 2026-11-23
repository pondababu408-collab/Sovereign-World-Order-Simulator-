package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import com.example.engine.SimulationEngine
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun WorldMapScreen(
    state: GameState,
    selectedProvinceId: String?,
    selectedCountryIso: String?,
    onSelectProvince: (String) -> Unit,
    onSelectCountry: (String) -> Unit,
    onTravelToRegion: (String) -> Unit,
    onProposeSummit: (SummitOffer) -> String
) {
    val activeProvince = remember(selectedProvinceId, state.provinces, state.player.currentRegionId) {
        state.provinces.firstOrNull { it.id == selectedProvinceId }
            ?: state.provinces.firstOrNull { it.id == state.player.currentRegionId }
            ?: state.provinces.first()
    }
    val activeCountry = remember(selectedCountryIso, activeProvince, state.countries) {
        val iso = selectedCountryIso ?: activeProvince.countryIso
        state.countries.firstOrNull { it.iso == iso } ?: state.countries.first()
    }
    val countryProvinces = remember(activeCountry.iso, state.provinces) {
        state.provinces.filter { it.countryIso == activeCountry.iso }
    }

    var showSearchOverlay by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var sheetExpanded by remember { mutableStateOf(false) }
    var showSummitDialog by remember { mutableStateOf(false) }
    var summitResult by remember { mutableStateOf<String?>(null) }
    var focusTrigger by remember { mutableIntStateOf(0) }

    BackHandler(enabled = sheetExpanded || showSearchOverlay) {
        if (showSearchOverlay) showSearchOverlay = false
        else if (sheetExpanded) sheetExpanded = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
    ) {
        // Full-bleed interactive GeoJSON Vector World Map
        WorldMapCanvas(
            countries = state.countries,
            provinces = state.provinces,
            selectedProvinceId = activeProvince.id,
            selectedCountryIso = activeCountry.iso,
            homeCountryIso = state.player.countryIso,
            playerRegionId = state.player.currentRegionId,
            focusTrigger = focusTrigger,
            onProvinceTapped = { prov ->
                onSelectProvince(prov.id)
                sheetExpanded = true
            },
            onCountryTapped = { iso ->
                onSelectCountry(iso)
            },
            onSearchClicked = { showSearchOverlay = !showSearchOverlay },
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )

        // Floating Search Overlay when Search Circle Button clicked
        if (showSearchOverlay) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OledCard.copy(alpha = 0.96f))
                    .border(1.dp, OledBorderHighlight, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search any 195+ country, city or enclave...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showSearchOverlay = false; searchText = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close Search", tint = TextSecondary)
                    }
                }
                val matches = remember(searchText, state.countries, state.provinces) {
                    if (searchText.isBlank()) {
                        state.countries.take(6).mapNotNull { c ->
                            state.provinces.firstOrNull { it.countryIso == c.iso }?.let { c to it }
                        }
                    } else {
                        state.provinces.filter {
                            it.name.contains(searchText, true) ||
                                it.capitalCity.contains(searchText, true) ||
                                it.countryIso.equals(searchText, true)
                        }.take(8).mapNotNull { p ->
                            state.countries.firstOrNull { it.iso == p.countryIso }?.let { it to p }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                matches.forEach { (c, p) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectProvince(p.id)
                                focusTrigger++
                                showSearchOverlay = false
                                sheetExpanded = true
                                searchText = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OledBadge(c.iso, AccentCyan)
                            Text("${c.name} • ${p.capitalCity}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                        Text("REL ${c.bilateralRelation}", style = MaterialTheme.typography.labelSmall, color = if (c.bilateralRelation >= 0) AccentGreen else AccentRed)
                    }
                }
            }
        }

        // Bottom Sleek Country Pill & Expandable Dossier Sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            if (!sheetExpanded) {
                Row(
                    modifier = Modifier
                        .testTag("map_country_pill")
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(OledCard.copy(alpha = 0.95f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .clickable { sheetExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        OledBadge(activeCountry.iso, AccentGreen)
                        Column {
                            Text(
                                text = activeCountry.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${activeProvince.capitalCity} • ${activeCountry.currencyCode} • REL ${activeCountry.bilateralRelation}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextDescription,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Inspect", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Expand Country Dossier", tint = AccentGreen)
                    }
                }
            } else {
                val isForeign = activeCountry.iso != state.player.countryIso
                val canDiplomacy = SimulationEngine.hasPortfolioAccess(state, MinistryTag.FOREIGN_AFFAIRS) ||
                    SimulationEngine.hasPortfolioAccess(state, MinistryTag.HEAD_OF_STATE)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OledCard.copy(alpha = 0.97f))
                        .border(1.dp, AccentGreen.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            OledBadge(activeCountry.iso, AccentGreen)
                            Column {
                                Text(activeCountry.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                Text("${activeCountry.govType.label} • ${activeCountry.economicSystem.label}", style = MaterialTheme.typography.labelSmall, color = AccentGold)
                            }
                        }
                        IconButton(onClick = { sheetExpanded = false }) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Collapse Dossier", tint = TextSecondary)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            OledCard(containerColor = OledElevated) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("${activeProvince.name} (${activeProvince.capitalCity})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                            if (activeProvince.isMicroStateOrEnclave) OledBadge("ENCLAVE", AccentGold)
                                        }
                                        Text("Pop: ${String.format("%.2fM", activeProvince.populationMillions)} • White ${String.format("%.2fx", activeProvince.whiteMarketIndex)} • Black ${String.format("%.2fx", activeProvince.blackMarketPremium)}", style = MaterialTheme.typography.labelSmall, color = TextDescription)
                                        Text("Grid: ${activeProvince.powerSupplyMw}/${activeProvince.powerDemandMw} MW", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                    }
                                    OledPrimaryButton(
                                        text = if (state.player.currentRegionId == activeProvince.id) "Here" else "Travel",
                                        enabled = state.player.currentRegionId != activeProvince.id,
                                        onClick = { onTravelToRegion(activeProvince.id) },
                                        accent = AccentGreen
                                    )
                                }
                            }
                        }

                        if (countryProvinces.size > 1) {
                            item {
                                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    countryProvinces.forEach { prov ->
                                        FilterChip(
                                            selected = prov.id == activeProvince.id,
                                            onClick = { onSelectProvince(prov.id) },
                                            label = { Text(prov.capitalCity, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            OledCard(containerColor = OledElevated) {
                                Text("Legislature: ${activeCountry.lowerChamberName} / ${activeCountry.upperChamberName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("Central Bank: ${activeCountry.centralBankName} (${activeCountry.currencyCode} • ${String.format("%.2f", activeCountry.forexPerUsd)}/USD)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("Intel: ${activeCountry.intelAgencyName} • Bloc: ${activeCountry.allianceBloc}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("Agenda: ${activeCountry.nationalAgenda}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                Spacer(Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("GDP: $${String.format("%.1fB", activeCountry.gdpBillionsUsd)}", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                                    Text("Stability: ${activeCountry.stability}%", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                                    Text(if (activeCountry.visaExemption) "Visa: EXEMPT" else "Visa: REQUIRED", style = MaterialTheme.typography.labelMedium, color = if (activeCountry.visaExemption) AccentGreen else AccentRed)
                                }
                            }
                        }

                        if (isForeign) {
                            item {
                                OledPrimaryButton(
                                    text = if (canDiplomacy) "Host Bilateral Summit & Counter-Offer" else "Summit Locked ([FOREIGN_AFFAIRS] Required)",
                                    onClick = { showSummitDialog = true },
                                    enabled = canDiplomacy,
                                    modifier = Modifier.fillMaxWidth(),
                                    accent = AccentGold
                                )
                            }
                        }

                        item { Text("Quick Bilateral Switch", style = MaterialTheme.typography.labelSmall, color = TextMuted) }
                        items(state.countries.filter { it.iso != state.player.countryIso }.take(12), key = { it.iso }) { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OledElevated)
                                    .border(1.dp, OledBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectCountry(c.iso)
                                        focusTrigger++
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${c.name} (${c.iso}) • ${c.allianceBloc}", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                                OledBadge("REL ${c.bilateralRelation}", if (c.bilateralRelation >= 30) AccentGreen else if (c.bilateralRelation < 0) AccentRed else TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSummitDialog) {
        var visaTreaty by remember { mutableStateOf(!activeCountry.visaExemption) }
        var armsDeal by remember { mutableStateOf(false) }
        var currencySwap by remember { mutableStateOf(true) }
        var foreignAid by remember { mutableStateOf(false) }
        var allianceInvite by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSummitDialog = false },
            containerColor = OledCard,
            title = { Text("Bilateral Summit: ${activeCountry.name}", style = MaterialTheme.typography.titleMedium, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current Relation: ${activeCountry.bilateralRelation} • Bloc: ${activeCountry.allianceBloc}", style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                    SummitToggle("Visa Exemption Treaty", visaTreaty) { visaTreaty = it }
                    SummitToggle("Bulk Weapons Deal ($120M)", armsDeal) { armsDeal = it }
                    SummitToggle("Central Bank Currency Swap ($250M)", currencySwap) { currencySwap = it }
                    SummitToggle("Foreign Development Aid ($80M)", foreignAid) { foreignAid = it }
                    SummitToggle("Military / Economic Alliance Invite", allianceInvite) { allianceInvite = it }
                    summitResult?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = AccentGold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    summitResult = onProposeSummit(
                        SummitOffer(
                            id = "sum_${System.currentTimeMillis()}",
                            targetCountryIso = activeCountry.iso,
                            visaExemptionProposed = visaTreaty,
                            armsDealMillionsUsd = if (armsDeal) 120.0 else 0.0,
                            currencySwapMillionsUsd = if (currencySwap) 250.0 else 0.0,
                            foreignAidMillionsUsd = if (foreignAid) 80.0 else 0.0,
                            allianceInvite = allianceInvite
                        )
                    )
                }) { Text("Submit Proposal", color = AccentCyan) }
            },
            dismissButton = {
                TextButton(onClick = { showSummitDialog = false; summitResult = null }) { Text("Close", color = TextSecondary) }
            }
        )
    }
}

@Composable
private fun SummitToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

