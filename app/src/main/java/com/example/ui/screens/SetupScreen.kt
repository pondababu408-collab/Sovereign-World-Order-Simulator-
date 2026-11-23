package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SaveSlotSummary
import com.example.data.WorldCatalog
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SetupScreen(
    saveSlots: List<SaveSlotSummary>,
    onStartCampaign: (
        name: String,
        handle: String,
        countryIso: String,
        regionId: String,
        mode: StartingMode,
        attributes: CharacterAttributes,
        ideology: IdeologyProfile
    ) -> Unit,
    onLoadSlot: (Int) -> Unit,
    onDeleteSlot: (Int) -> Unit
) {
    val allCountries = remember { WorldCatalog.buildAllCountries("IN") }
    val allProvinces = remember(allCountries) { WorldCatalog.buildProvinces(allCountries) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(allCountries.first { it.iso == "IN" }) }
    val countryProvinces = remember(selectedCountry) {
        allProvinces.filter { it.countryIso == selectedCountry.iso }
    }
    var selectedProvince by remember(selectedCountry) {
        mutableStateOf(countryProvinces.firstOrNull() ?: allProvinces.first())
    }
    var selectedMode by remember { mutableStateOf(StartingMode.CAREER) }
    var playerName by remember { mutableStateOf("Aarav Vance") }
    var playerHandle by remember { mutableStateOf("aarav_vance") }
    var culture by remember { mutableStateOf("Cosmopolitan") }
    var belief by remember { mutableStateOf("Secular Pragmatism") }
    var primaryIdeology by remember { mutableStateOf("Technocratic Realism") }
    var showCountryPicker by remember { mutableStateOf(false) }
    var showLoadSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WORLD ORDER",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "OFFLINE GEOPOLITICAL & MACROECONOMIC SIM",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan
                )
            }
            OledOutlinedButton(
                text = "Load Slot",
                onClick = { showLoadSheet = true },
                accent = AccentGold
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mode Toggle
            item {
                OledCard {
                    OledSectionHeader("1. Campaign Mode", "Career vs Sandbox Executive Start", Icons.Filled.Flag)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StartingMode.entries.forEach { mode ->
                            val active = selectedMode == mode
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("mode_${mode.name.lowercase()}")
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) AccentCyan.copy(alpha = 0.14f) else OledElevated)
                                    .border(1.dp, if (active) AccentCyan else OledBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedMode = mode }
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) AccentCyan else TextPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = mode.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Country & Region Selector (195+ Real Countries)
            item {
                OledCard {
                    OledSectionHeader(
                        "2. Nation & Home Province",
                        "${allCountries.size} Sovereign States Available",
                        Icons.Filled.Public,
                        AccentGold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("country_picker_button")
                            .clip(RoundedCornerShape(8.dp))
                            .background(OledElevated)
                            .border(1.dp, OledBorderHighlight, RoundedCornerShape(8.dp))
                            .clickable { showCountryPicker = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${selectedCountry.name} (${selectedCountry.iso})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "${selectedCountry.govType.label} • ${selectedCountry.lowerChamberName} • ${selectedCountry.currencyCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Central Bank: ${selectedCountry.centralBankName} | Intel: ${selectedCountry.intelAgencyName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentCyan
                            )
                        }
                        OledBadge("CHANGE", AccentGold)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Starting Home Province", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        countryProvinces.forEach { prov ->
                            val active = selectedProvince.id == prov.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) AccentCyan.copy(alpha = 0.12f) else OledElevated)
                                    .border(1.dp, if (active) AccentCyan else OledBorder, RoundedCornerShape(6.dp))
                                    .clickable { selectedProvince = prov }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${prov.name} (${prov.capitalCity})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (active) AccentCyan else TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${prov.localPopsCount} Pops",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Executive Identity
            item {
                OledCard {
                    OledSectionHeader("3. Executive Identity & Ideology", "Character sheet & attributes")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ExecutivePortrait(seed = playerName.hashCode(), name = playerName, size = 56.dp)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = playerName,
                                onValueChange = { playerName = it },
                                label = { Text("Full Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
                            )
                            OutlinedTextField(
                                value = playerHandle,
                                onValueChange = { playerHandle = it.lowercase().replace(" ", "_") },
                                label = { Text("@username") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setup_handle_input")
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = culture,
                            onValueChange = { culture = it },
                            label = { Text("Culture") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = primaryIdeology,
                            onValueChange = { primaryIdeology = it },
                            label = { Text("Primary Ideology") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        OledPrimaryButton(
            text = if (selectedMode == StartingMode.CAREER) {
                "Launch Career (0.00 ${selectedCountry.currencyCode}) in ${selectedCountry.iso}"
            } else {
                "Inaugurate Head of State in ${selectedCountry.name}"
            },
            onClick = {
                onStartCampaign(
                    playerName,
                    playerHandle,
                    selectedCountry.iso,
                    selectedProvince.id,
                    selectedMode,
                    CharacterAttributes(culture = culture, belief = belief),
                    IdeologyProfile(
                        primary = primaryIdeology,
                        economicSystem = selectedCountry.economicSystem,
                        formOfGovernment = selectedCountry.govType,
                        powerStructure = selectedCountry.powerStructure
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("launch_campaign_button"),
            accent = AccentCyan
        )
    }

    if (showCountryPicker) {
        AlertDialog(
            onDismissRequest = { showCountryPicker = false },
            containerColor = OledCard,
            title = {
                Column {
                    Text("Select Nation (${allCountries.size} States)", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search India, USA, France, Spain, Cuba...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                val filtered = remember(searchQuery, allCountries) {
                    if (searchQuery.isBlank()) allCountries
                    else allCountries.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                            it.iso.contains(searchQuery, ignoreCase = true) ||
                            it.capital.contains(searchQuery, ignoreCase = true)
                    }
                }
                LazyColumn(modifier = Modifier.height(340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.iso }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(OledElevated)
                                .border(1.dp, if (c.iso == selectedCountry.iso) AccentCyan else OledBorder, RoundedCornerShape(6.dp))
                                .clickable {
                                    selectedCountry = c
                                    showCountryPicker = false
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${c.name} (${c.iso})", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                Text("${c.capital} • ${c.currencyCode} • ${c.govType.label}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OledBadge(c.economicSystem.label, AccentGold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCountryPicker = false }) { Text("Close", color = AccentCyan) }
            }
        )
    }

    if (showLoadSheet) {
        AlertDialog(
            onDismissRequest = { showLoadSheet = false },
            containerColor = OledCard,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Save, contentDescription = null, tint = AccentGold)
                    Spacer(Modifier.width(8.dp))
                    Text("Load Offline Campaign", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    saveSlots.forEach { slot ->
                        OledCard(containerColor = OledElevated) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (slot.isEmpty) "Slot ${slot.slotId} • Empty" else slot.campaignName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (slot.isEmpty) TextMuted else TextPrimary
                                    )
                                    if (!slot.isEmpty) {
                                        Text("${slot.countryName} • ${slot.dateFormatted}", style = MaterialTheme.typography.labelSmall, color = AccentCyan)
                                    }
                                }
                                if (!slot.isEmpty) {
                                    TextButton(onClick = {
                                        onLoadSlot(slot.slotId)
                                        showLoadSheet = false
                                    }) { Text("Load", color = AccentCyan) }
                                    TextButton(onClick = { onDeleteSlot(slot.slotId) }) { Text("Del", color = AccentRed) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLoadSheet = false }) { Text("Close", color = TextSecondary) }
            }
        )
    }
}
