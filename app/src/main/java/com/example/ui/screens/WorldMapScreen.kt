package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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

    var searchText by remember { mutableStateOf("") }
    var showSummitDialog by remember { mutableStateOf(false) }
    var summitResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OledSectionHeader(
                title = "Provincial World Map & Diplomacy",
                subtitle = "Located in ${state.player.currentRegionId} (${state.player.currentCountryIso})",
                icon = Icons.Filled.Public,
                accent = AccentCyan
            )
        }

        // Interactive Provincial World Map Canvas
        item {
            WorldMapCanvas(
                countries = state.countries,
                provinces = state.provinces,
                selectedProvinceId = activeProvince.id,
                homeCountryIso = state.player.countryIso,
                playerRegionId = state.player.currentRegionId,
                onProvinceTapped = { prov -> onSelectProvince(prov.id) }
            )
        }

        // Quick Search Countries / Enclaves
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Jump to country or enclave (Andorra, Ceuta, Gibraltar, Singapore, USA...)", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (searchText.isNotBlank()) {
                val matches = state.provinces.filter {
                    it.name.contains(searchText, true) || it.capitalCity.contains(searchText, true) || it.countryIso.equals(searchText, true)
                }.take(5)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                    matches.forEach { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OledElevated)
                                .border(1.dp, OledBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    onSelectProvince(p.id)
                                    searchText = ""
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${p.name} (${p.countryIso})", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                            Text(p.capitalCity, style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                        }
                    }
                }
            }
        }

        // Selected Province Inspector
        item {
            OledCard(borderColor = AccentCyan.copy(alpha = 0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(activeProvince.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            if (activeProvince.isMicroStateOrEnclave) OledBadge("ENCLAVE/MICRO", AccentGold)
                        }
                        Text("Capital: ${activeProvince.capitalCity} • Pops: ${activeProvince.localPopsCount} • Pop: ${String.format("%.2fM", activeProvince.populationMillions)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    OledPrimaryButton(
                        text = if (state.player.currentRegionId == activeProvince.id) "Here" else "Travel",
                        enabled = state.player.currentRegionId != activeProvince.id,
                        onClick = { onTravelToRegion(activeProvince.id) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("White Market", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${String.format("%.2fx", activeProvince.whiteMarketIndex)} Legal", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                    }
                    Column {
                        Text("Black Market", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${String.format("%.2fx", activeProvince.blackMarketPremium)} BTC/FX", style = MaterialTheme.typography.labelMedium, color = AccentBitcoin)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Electrical Grid", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("${activeProvince.powerSupplyMw}/${activeProvince.powerDemandMw} MW", style = MaterialTheme.typography.labelMedium, color = AccentCyan)
                    }
                }
            }
        }

        // Country Profile & Bilateral Diplomacy (-100 to +100)
        item {
            val isForeign = activeCountry.iso != state.player.countryIso
            val canDiplomacy = SimulationEngine.hasPortfolioAccess(state, MinistryTag.FOREIGN_AFFAIRS) ||
                SimulationEngine.hasPortfolioAccess(state, MinistryTag.HEAD_OF_STATE)

            OledCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${activeCountry.name} (${activeCountry.iso})", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text("${activeCountry.govType.label} • ${activeCountry.economicSystem.label}", style = MaterialTheme.typography.bodySmall, color = AccentGold)
                    }
                    val relCol = when {
                        activeCountry.bilateralRelation >= 35 -> AccentGreen
                        activeCountry.bilateralRelation <= -25 -> AccentRed
                        else -> TextSecondary
                    }
                    OledBadge("REL ${activeCountry.bilateralRelation}", relCol)
                }

                Spacer(Modifier.height(8.dp))
                Text("Legislature: ${activeCountry.lowerChamberName} / ${activeCountry.upperChamberName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Central Bank: ${activeCountry.centralBankName} (${activeCountry.currencyCode} • ${String.format("%.2f", activeCountry.forexPerUsd)}/USD)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Intelligence: ${activeCountry.intelAgencyName} • Bloc: ${activeCountry.allianceBloc}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Text("Agenda: ${activeCountry.nationalAgenda}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)

                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("GDP: $${String.format("%.1fB", activeCountry.gdpBillionsUsd)}", style = MaterialTheme.typography.labelMedium, color = AccentGreen)
                    Text("Stability: ${activeCountry.stability}%", style = MaterialTheme.typography.labelMedium, color = TextPrimary)
                    Text(if (activeCountry.visaExemption) "Visa: EXEMPT" else "Visa: REQUIRED", style = MaterialTheme.typography.labelMedium, color = if (activeCountry.visaExemption) AccentGreen else AccentRed)
                }

                if (countryProvinces.size > 1) {
                    Spacer(Modifier.height(8.dp))
                    Text("Provinces & Enclaves", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        countryProvinces.take(4).forEach { prov ->
                            FilterChip(
                                selected = prov.id == activeProvince.id,
                                onClick = { onSelectProvince(prov.id) },
                                label = { Text(prov.capitalCity, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (isForeign) {
                    Spacer(Modifier.height(10.dp))
                    OledPrimaryButton(
                        text = if (canDiplomacy) "Host Bilateral Summit & Counter-Offer" else "Summit Locked ([FOREIGN_AFFAIRS] Required)",
                        onClick = { showSummitDialog = true },
                        enabled = canDiplomacy,
                        modifier = Modifier.fillMaxWidth(),
                        accent = AccentGold
                    )
                }
            }
        }

        // Quick Foreign Relations Roster
        items(state.countries.filter { it.iso != state.player.countryIso }.take(15), key = { it.iso }) { c ->
            OledCard(onClick = { onSelectCountry(c.iso) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${c.name} (${c.iso})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text("${c.allianceBloc} • ${c.currencyCode} • SWIFT: ${if (c.swiftConnected) "Active" else "Sanctioned"}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    OledBadge("REL ${c.bilateralRelation}", if (c.bilateralRelation >= 30) AccentGreen else if (c.bilateralRelation < 0) AccentRed else TextSecondary)
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
